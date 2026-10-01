package com.tenantsphere.admin;

import com.tenantsphere.profile.AvatarStorage;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class AdminDeleteService {
    private final JdbcTemplate db;
    private final AdminPermissions access;
    private final AdminQueryService queries;
    private final AdminRecordService writes;
    private final AdminAuditService audit;
    private final AvatarStorage avatars;

    public AdminDeleteService(
            JdbcTemplate db,
            AdminPermissions access,
            AdminQueryService queries,
            AdminRecordService writes,
            AdminAuditService audit,
            AvatarStorage avatars) {
        this.db = db;
        this.access = access;
        this.queries = queries;
        this.writes = writes;
        this.audit = audit;
        this.avatars = avatars;
    }

    public record Preview(
            Map<String, Integer> counts,
            List<String> examples,
            List<String> effects,
            String fingerprint)
            implements java.io.Serializable {}

    @Transactional(readOnly = true)
    public Preview preview(AdminResource r, Map<Long, String> selection) {
        validateSelection(r, selection);
        return describe(r, selection, affected(r, selection.keySet()));
    }

    @Transactional
    public void delete(AdminResource r, Map<Long, String> selection, String confirmedFingerprint) {
        validateSelection(r, selection);
        writes.serializeAdministration();
        validateSelection(r, selection);
        for (long id : new TreeSet<>(selection.keySet())) writes.locked(r, id, selection.get(id));
        var affected = affected(r, selection.keySet());
        Preview latest = describe(r, selection, affected);
        if (!latest.fingerprint().equals(confirmedFingerprint))
            throw new IllegalArgumentException(
                    "The affected records have changed. Review the deletion again.");

        Set<Long> targets = new TreeSet<>();
        for (long report : affected.getOrDefault(AdminResource.REPORTS, Set.of()))
            targets.add(
                    db.queryForObject(
                            "SELECT reported_user_pkid FROM reports WHERE pkid=?",
                            Long.class,
                            report));
        writes.lockUsers(targets);
        var previousCounts = writes.reportCounts(targets);
        // Vote operations lock the post before changing its vote rows. Take that same lock
        // before cascading a deleted voter's rows, then recount using a fresh SQL snapshot.
        List<Long> votedPosts =
                r == AdminResource.USERS
                        ? db.queryForList(
                                "SELECT DISTINCT post_pkid FROM (SELECT post_pkid FROM post_upvotes"
                                    + " WHERE user_pkid IN "
                                        + ids(selection.keySet())
                                        + " UNION SELECT post_pkid FROM post_downvotes WHERE"
                                        + " user_pkid IN "
                                        + ids(selection.keySet())
                                        + ") votes ORDER BY post_pkid",
                                Long.class)
                        : List.of();
        for (long post : votedPosts)
            db.queryForList("SELECT pkid FROM posts WHERE pkid=? FOR UPDATE", post);
        // Lock and audit the complete cascade before the first DELETE. History is never cascaded
        // away.
        for (var entry : affected.entrySet())
            for (long id : new TreeSet<>(entry.getValue())) {
                db.queryForList(
                        "SELECT pkid FROM " + entry.getKey().table + " WHERE pkid=? FOR UPDATE",
                        id);
                var record = queries.readInternal(entry.getKey(), id);
                if (entry.getKey() == AdminResource.PROFILES)
                    avatars.removeAfterCommit((String) record.get("avatar"));
                audit.record(
                        entry.getKey(),
                        id,
                        entry.getKey() == r ? "delete" : "cascade_delete",
                        record,
                        null);
            }
        if (r == AdminResource.PROFILES) {
            access.require(AdminResource.USERS, "change");
            for (long id : selection.keySet()) {
                long owner =
                        db.queryForObject(
                                "SELECT user_pkid FROM profiles WHERE pkid=?", Long.class, id);
                var before = queries.readInternal(AdminResource.USERS, owner);
                writes.protectUser(before);
                writes.protectUserFlags(before, Map.of("is_active", false));
                db.update(
                        "UPDATE users SET"
                            + " is_active=FALSE,auth_version=auth_version+1,updated_at=clock_timestamp()"
                            + " WHERE pkid=?",
                        owner);
                audit.record(
                        AdminResource.USERS,
                        owner,
                        "deactivate",
                        before,
                        queries.readInternal(AdminResource.USERS, owner));
            }
        }
        for (long id : new TreeSet<>(selection.keySet()))
            db.update("DELETE FROM " + r.table + " WHERE pkid=?", id);
        writes.recalculateReports(targets, previousCounts);
        for (long post : votedPosts) {
            // Join-table rows cascade on account deletion; keep the cached feed totals consistent.
            db.update(
                    """
                    UPDATE posts p SET upvotes=(SELECT count(*) FROM post_upvotes v WHERE v.post_pkid=p.pkid),
                        downvotes=(SELECT count(*) FROM post_downvotes v WHERE v.post_pkid=p.pkid)
                    WHERE p.pkid=?
                    """,
                    post);
        }
    }

    private void validateSelection(AdminResource r, Map<Long, String> selection) {
        access.require(r, "delete");
        if (selection.isEmpty() || selection.size() > 100)
            throw new IllegalArgumentException("Select between 1 and 100 records.");
        if (r == AdminResource.GROUPS) access.requireSuperuser();
        for (long id : selection.keySet()) {
            var row = queries.readInternal(r, id);
            if (!Objects.equals(row.get("revision"), selection.get(id)))
                throw new IllegalArgumentException(
                        "A selected record changed. Refresh the list before deleting.");
            if (r == AdminResource.USERS) {
                writes.protectUser(row);
                if (id == access.actor().pkid())
                    throw new IllegalArgumentException("You cannot delete your own account.");
                writes.protectUserFlags(row, Map.of("is_active", false));
            }
            if (r == AdminResource.PROFILES) {
                access.require(AdminResource.USERS, "change");
                var owner =
                        queries.readInternal(
                                AdminResource.USERS,
                                AdminRecordService.number(row.get("user_pkid")));
                writes.protectUser(owner);
                writes.protectUserFlags(owner, Map.of("is_active", false));
            }
        }
    }

    private EnumMap<AdminResource, Set<Long>> affected(AdminResource r, Set<Long> roots) {
        var out = new EnumMap<AdminResource, Set<Long>>(AdminResource.class);
        out.put(r, new TreeSet<>(roots));
        if (r == AdminResource.USERS) {
            String ids = ids(roots);
            add(out, AdminResource.PROFILES, "user_pkid IN " + ids);
            add(out, AdminResource.POSTS, "author_pkid IN " + ids);
            add(out, AdminResource.ISSUES, "reported_by_pkid IN " + ids);
            add(
                    out,
                    AdminResource.RATINGS,
                    "rated_user_pkid IN " + ids + " OR rating_user_pkid IN " + ids);
            add(
                    out,
                    AdminResource.REPORTS,
                    "reported_by_pkid IN " + ids + " OR reported_user_pkid IN " + ids);
        }
        if (r == AdminResource.APARTMENTS)
            add(out, AdminResource.ISSUES, "apartment_pkid IN " + ids(roots));
        if (out.containsKey(AdminResource.POSTS))
            add(
                    out,
                    AdminResource.CONTENT_VIEWS,
                    "content_type='post' AND object_pkid IN " + ids(out.get(AdminResource.POSTS)));
        if (out.containsKey(AdminResource.ISSUES))
            add(
                    out,
                    AdminResource.CONTENT_VIEWS,
                    "content_type='issue' AND object_pkid IN "
                            + ids(out.get(AdminResource.ISSUES)));
        if (out.values().stream().mapToInt(Set::size).sum() > 5000)
            throw new IllegalArgumentException(
                    "This selection affects more than 5,000 records. Delete a smaller batch.");
        for (var child : out.entrySet())
            if (!child.getValue().isEmpty()) access.require(child.getKey(), "delete");
        return out;
    }

    private Preview describe(
            AdminResource r, Map<Long, String> roots, EnumMap<AdminResource, Set<Long>> affected) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        List<String> examples = new ArrayList<>();
        List<String> effects = new ArrayList<>();
        StringBuilder revision = new StringBuilder();
        for (var entry : affected.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            counts.put(entry.getKey().title, entry.getValue().size());
            for (var row :
                    db.queryForList(
                            "SELECT pkid,updated_at::text AS revision,"
                                    + entry.getKey().labelColumn
                                    + "::text AS label FROM "
                                    + entry.getKey().table
                                    + " WHERE pkid IN "
                                    + ids(entry.getValue())
                                    + " ORDER BY pkid")) {
                revision.append(entry.getKey()).append(row);
                if (entry.getKey() == r && examples.size() < 12)
                    examples.add(row.get("label") + " (#" + row.get("pkid") + ")");
            }
        }
        String replyWhere = null;
        if (affected.containsKey(AdminResource.POSTS))
            replyWhere = "post_pkid IN " + ids(affected.get(AdminResource.POSTS));
        if (r == AdminResource.USERS)
            replyWhere =
                    (replyWhere == null ? "" : replyWhere + " OR ")
                            + "author_pkid IN "
                            + ids(roots.keySet());
        if (replyWhere != null) {
            var replies =
                    db.queryForList(
                            "SELECT pkid,updated_at::text FROM replies WHERE "
                                    + replyWhere
                                    + " ORDER BY pkid");
            if (!replies.isEmpty()) {
                counts.put("Replies", replies.size());
                revision.append(replies);
            }
        }
        if (r == AdminResource.USERS) {
            counts.put(
                    "Votes and bookmarks",
                    count("post_upvotes", "user_pkid IN " + ids(roots.keySet()))
                            + count("post_downvotes", "user_pkid IN " + ids(roots.keySet()))
                            + count("post_bookmarks", "user_pkid IN " + ids(roots.keySet())));
            effects.add(
                    "Apartment tenants, issue assignees and surviving content-view users will be"
                            + " cleared where they reference these accounts.");
            effects.add(
                    "Report counts and post vote totals will be recalculated. Administration"
                            + " history will be retained.");
        }
        if (affected.containsKey(AdminResource.POSTS))
            effects.add(
                    "Replies, tags attached to these posts, votes and bookmarks are removed with"
                            + " their posts.");
        if (r == AdminResource.PROFILES)
            effects.add(
                    "The associated accounts will be deactivated until a profile is recreated and"
                            + " an administrator reactivates them.");
        if (r == AdminResource.REPORTS)
            effects.add(
                    "Distinct reporter counts and reputations will be recalculated. Accounts are"
                            + " not automatically reactivated.");
        if (r == AdminResource.TAGS)
            effects.add("This tag will be detached from its posts. The posts remain available.");
        if (r == AdminResource.GROUPS)
            effects.add(
                    "Members lose permissions inherited from these groups. Direct permissions and"
                            + " other groups remain.");
        revision.append(counts);
        try {
            return new Preview(
                    counts,
                    examples,
                    effects,
                    HexFormat.of()
                            .formatHex(
                                    MessageDigest.getInstance("SHA-256")
                                            .digest(
                                                    revision.toString()
                                                            .getBytes(StandardCharsets.UTF_8))));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private int count(String table, String where) {
        return db.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE " + where, Integer.class);
    }

    private void add(EnumMap<AdminResource, Set<Long>> out, AdminResource r, String where) {
        out.computeIfAbsent(r, key -> new TreeSet<>())
                .addAll(
                        db.queryForList(
                                "SELECT pkid FROM " + r.table + " WHERE " + where, Long.class));
    }

    private static String ids(Collection<Long> values) {
        return values.isEmpty()
                ? "(NULL)"
                : "("
                        + String.join(",", values.stream().sorted().map(String::valueOf).toList())
                        + ")";
    }
}
