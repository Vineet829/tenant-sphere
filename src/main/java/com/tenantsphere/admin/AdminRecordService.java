package com.tenantsphere.admin;

import com.tenantsphere.common.AfterCommit;
import com.tenantsphere.common.Slugs;
import com.tenantsphere.mail.MailService;
import com.tenantsphere.profile.AvatarService;
import com.tenantsphere.profile.AvatarStorage;
import com.tenantsphere.profile.Profile;
import com.tenantsphere.user.UserRepository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

/** All administration writes are validated, authorized, transactional and audited here. */
@Service
public class AdminRecordService {
    private final JdbcTemplate db;
    private final AdminPermissions access;
    private final AdminQueryService queries;
    private final AdminAuditService audit;
    private final PasswordEncoder passwords;
    private final MailService mail;
    private final AvatarService avatars;
    private final AvatarStorage avatarStorage;
    private final UserRepository users;

    public AdminRecordService(
            JdbcTemplate db,
            AdminPermissions access,
            AdminQueryService queries,
            AdminAuditService audit,
            PasswordEncoder passwords,
            MailService mail,
            AvatarService avatars,
            UserRepository users,
            AvatarStorage avatarStorage) {
        this.db = db;
        this.access = access;
        this.queries = queries;
        this.audit = audit;
        this.passwords = passwords;
        this.mail = mail;
        this.avatars = avatars;
        this.users = users;
        this.avatarStorage = avatarStorage;
    }

    @Transactional
    public long save(AdminResource r, Long id, String revision, Map<String, Object> input) {
        access.require(r, id == null ? "add" : "change");
        serializeAdministration();
        access.require(r, id == null ? "add" : "change");
        Map<String, Object> before = id == null ? null : locked(r, id, revision);
        if (r == AdminResource.GROUPS) access.requireSuperuser();
        if (r == AdminResource.USERS && before != null) protectUser(before);
        var values = validate(r, input, id == null, before);
        if (r == AdminResource.PROFILES
                && before != null
                && !Objects.equals(
                        number(before.get("user_pkid")), number(values.get("user_pkid"))))
            throw new IllegalArgumentException(
                    "A profile cannot be moved to another account. Create a profile for that"
                            + " account instead.");

        Set<Long> reportedUsers = new TreeSet<>();
        if (r == AdminResource.REPORTS) {
            reportedUsers.add(number(values.get("reported_user_pkid")));
            if (before != null) reportedUsers.add(number(before.get("reported_user_pkid")));
            lockUsers(reportedUsers);
        }
        Map<Long, Integer> oldCounts = reportCounts(reportedUsers);

        Map<String, Object> sql = new LinkedHashMap<>(values);
        sql.remove("tags");
        sql.remove("groups");
        sql.remove("permissions");
        sql.remove("password_confirmation");
        if (r == AdminResource.USERS) {
            String password = (String) sql.remove("password");
            if (password != null && !password.isBlank())
                sql.put("password", passwords.encode(password));
            if (before != null) {
                if (!access.actor().superuser()) {
                    sql.remove("is_active");
                    sql.remove("is_staff");
                    sql.remove("is_superuser");
                }
                protectUserFlags(before, sql);
                if (sql.containsKey("password")
                        || (Boolean.TRUE.equals(before.get("is_active"))
                                && Boolean.FALSE.equals(sql.get("is_active")))) {
                    sql.put(
                            "auth_version",
                            db.queryForObject(
                                    "SELECT auth_version+1 FROM users WHERE pkid=?",
                                    Long.class,
                                    id));
                }
            }
        }
        if (r == AdminResource.ISSUES) {
            boolean resolved = "resolved".equals(sql.get("status"));
            sql.put(
                    "resolved_on",
                    resolved
                            ? (before != null && before.get("resolved_on") != null
                                    ? before.get("resolved_on")
                                    : java.sql.Date.valueOf(LocalDate.now()))
                            : null);
        }
        if (id == null) {
            sql.put("id", UUID.randomUUID());
            if (Set.of(
                            AdminResource.POSTS,
                            AdminResource.REPORTS,
                            AdminResource.PROFILES,
                            AdminResource.TAGS)
                    .contains(r)) {
                String base =
                        r == AdminResource.PROFILES
                                ? queries.choice("users", number(values.get("user_pkid")))
                                        .label()
                                        .split(" · ")[0]
                                : values.get(r == AdminResource.TAGS ? "name" : "title").toString();
                sql.put(
                        "slug",
                        uniqueSlug(
                                r.table,
                                r == AdminResource.TAGS
                                        ? base.substring(0, Math.min(65, base.length()))
                                        : base));
            }
            String columns = String.join(",", sql.keySet());
            id =
                    db.queryForObject(
                            "INSERT INTO "
                                    + r.table
                                    + " ("
                                    + columns
                                    + ") VALUES ("
                                    + String.join(",", Collections.nCopies(sql.size(), "?"))
                                    + ") RETURNING pkid",
                            Long.class,
                            sql.values().toArray());
        } else {
            var args = new ArrayList<>(sql.values());
            args.add(id);
            db.update(
                    "UPDATE "
                            + r.table
                            + " SET "
                            + String.join(",", sql.keySet().stream().map(k -> k + "=?").toList())
                            + ",updated_at=clock_timestamp() WHERE pkid=?",
                    args.toArray());
        }
        if (r == AdminResource.USERS) {
            if (before == null) {
                db.update(
                        "INSERT INTO profiles(id,user_pkid,slug) VALUES(?,?,?)",
                        UUID.randomUUID(),
                        id,
                        uniqueSlug("profiles", values.get("username").toString()));
            }
            if (access.actor().superuser()) {
                if (input.containsKey("groups")) replaceGroups(id, input.get("groups"));
                if (input.containsKey("permissions"))
                    replacePermissions(
                            "admin_user_permissions", "user_pkid", id, input.get("permissions"));
            }
            if (values.get("password") != null && !values.get("password").toString().isBlank()) {
                db.update(
                        "UPDATE verification_tokens SET used_at=NOW() WHERE user_pkid=? AND used_at"
                                + " IS NULL",
                        id);
            }
        }
        if (r == AdminResource.GROUPS && input.containsKey("permissions"))
            replacePermissions(
                    "admin_group_permissions", "group_pkid", id, input.get("permissions"));
        if (r == AdminResource.POSTS) saveTags(id, Objects.toString(values.get("tags"), ""));
        if (r == AdminResource.PROFILES)
            recalculateReports(Set.of(number(values.get("user_pkid"))), Map.of());
        // Taking away the staff role that kept a reported account on applies the
        // five-reporter rule again, as a fifth report would. Only then: a recount
        // moves the profile's version, which would interrupt an open profile edit.
        if (r == AdminResource.USERS
                && before != null
                && Profile.keptOnByStaffRole(
                        Boolean.TRUE.equals(before.get("is_staff")),
                        Boolean.TRUE.equals(before.get("is_superuser")))
                && !Profile.keptOnByStaffRole(
                        Boolean.TRUE.equals(sql.getOrDefault("is_staff", before.get("is_staff"))),
                        Boolean.TRUE.equals(sql.getOrDefault("is_superuser", before.get("is_superuser")))))
            recalculateReports(Set.of(id), Map.of());
        recalculateReports(reportedUsers, oldCounts);
        var after = queries.readInternal(r, id);
        if (r == AdminResource.USERS
                && values.get("password") != null
                && !values.get("password").toString().isBlank())
            after.put("password_changed", true);
        audit.record(r, id, before == null ? "add" : "change", before, after);
        if (r == AdminResource.USERS && before == null) {
            long profile =
                    db.queryForObject(
                            "SELECT pkid FROM profiles WHERE user_pkid=?", Long.class, id);
            audit.record(
                    AdminResource.PROFILES,
                    profile,
                    "add",
                    null,
                    queries.readInternal(AdminResource.PROFILES, profile));
        }
        if (r == AdminResource.ISSUES) notifyIssue(before, after);
        return id;
    }

    @Transactional
    public void uploadAvatar(long profileId, String revision, MultipartFile file) {
        access.require(AdminResource.PROFILES, "change");
        serializeAdministration();
        var before = locked(AdminResource.PROFILES, profileId, revision);
        var owner = users.findById(number(before.get("user_pkid"))).orElseThrow();
        avatars.upload(owner, file);
        // AvatarStorage participates in this transaction and cleans up files on rollback.
        db.update("UPDATE profiles SET updated_at=clock_timestamp() WHERE pkid=?", profileId);
        // Flush the JPA write before reading through JDBC for the audit snapshot.
        users.flush();
        audit.record(
                AdminResource.PROFILES,
                profileId,
                "avatar",
                before,
                queries.readInternal(AdminResource.PROFILES, profileId));
    }

    @Transactional
    public void clearAvatar(long profileId, String revision) {
        access.require(AdminResource.PROFILES, "change");
        serializeAdministration();
        var before = locked(AdminResource.PROFILES, profileId, revision);
        db.update(
                "UPDATE profiles SET avatar=NULL,updated_at=clock_timestamp() WHERE pkid=?",
                profileId);
        avatarStorage.removeAfterCommit((String) before.get("avatar"));
        audit.record(
                AdminResource.PROFILES,
                profileId,
                "avatar",
                before,
                queries.readInternal(AdminResource.PROFILES, profileId));
    }

    @Transactional
    public void bulkIssueStatus(Map<Long, String> selection, String status) {
        access.require(AdminResource.ISSUES, "change");
        if (selection.isEmpty() || selection.size() > 100)
            throw new IllegalArgumentException("Select between 1 and 100 issues.");
        for (long id : new TreeSet<>(selection.keySet())) {
            var row = queries.readInternal(AdminResource.ISSUES, id);
            row.put("status", status);
            save(AdminResource.ISSUES, id, selection.get(id), row);
        }
    }

    Map<String, Object> locked(AdminResource r, long id, String revision) {
        var rows = db.queryForList("SELECT pkid FROM " + r.table + " WHERE pkid=? FOR UPDATE", id);
        if (rows.isEmpty())
            throw new IllegalArgumentException("This record has been deleted. Refresh the list.");
        var current = queries.readInternal(r, id);
        if (!Objects.equals(current.get("revision"), revision))
            throw new IllegalArgumentException(
                    "This record changed while you were editing. Close and reopen it to load the"
                            + " latest values.");
        return current;
    }

    void serializeAdministration() {
        db.queryForObject("SELECT pg_advisory_xact_lock(170628,4)", Object.class);
    }

    void protectUser(Map<String, Object> target) {
        if ((Boolean.TRUE.equals(target.get("is_staff"))
                        || Boolean.TRUE.equals(target.get("is_superuser")))
                && !access.actor().superuser())
            throw new AccessDeniedException("Only superusers may edit or delete staff accounts.");
    }

    void protectUserFlags(Map<String, Object> before, Map<String, Object> values) {
        boolean active = (boolean) values.getOrDefault("is_active", before.get("is_active"));
        boolean staff = (boolean) values.getOrDefault("is_staff", before.get("is_staff"));
        boolean superuser =
                (boolean) values.getOrDefault("is_superuser", before.get("is_superuser"));
        if (number(before.get("pkid")) == access.actor().pkid()
                && (!active
                        || (!staff && !superuser)
                        || (Boolean.TRUE.equals(before.get("is_superuser")) && !superuser)))
            throw new IllegalArgumentException("You cannot remove your own administration access.");
        // Only turning a resident's account back on waits for their reports
        // to be reviewed. Reports never turn a staff account off, so they
        // never keep one off either.
        if (active
                && !Boolean.TRUE.equals(before.get("is_active"))
                && !Profile.keptOnByStaffRole(staff, superuser)
                && db.queryForObject(
                                "SELECT COALESCE(MAX(report_count),0) FROM profiles WHERE"
                                        + " user_pkid=?",
                                Integer.class,
                                before.get("pkid"))
                        >= 5)
            throw new IllegalArgumentException(
                    "This account has reports from five different residents. Review and remove"
                            + " invalid reports before reactivating it.");
        if (Boolean.TRUE.equals(before.get("is_superuser"))
                && Boolean.TRUE.equals(before.get("is_active"))
                && (!active || !superuser)
                && db.queryForObject(
                                "SELECT COUNT(*) FROM users WHERE is_active AND is_superuser",
                                Integer.class)
                        <= 1)
            throw new IllegalArgumentException("Keep at least one active superuser.");
    }

    private Map<String, Object> validate(
            AdminResource r,
            Map<String, Object> input,
            boolean create,
            Map<String, Object> before) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (var f : r.fields) {
            Object value = input.get(f.key());
            if (value instanceof String text && f.kind() != AdminResource.Kind.PASSWORD)
                value = text.trim();
            if (f.required() && (value == null || value.toString().isBlank()))
                throw new IllegalArgumentException(f.label() + " is required.");
            if (value == null || value.toString().isBlank()) {
                out.put(
                        f.key(),
                        f.kind() == AdminResource.Kind.BOOLEAN
                                ? false
                                : f.kind() == AdminResource.Kind.AREA ? "" : null);
                continue;
            }
            switch (f.kind()) {
                case INTEGER, RELATION -> {
                    try {
                        value = Long.valueOf(value.toString());
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(f.label() + " must be a whole number.");
                    }
                    if ((long) value < 0
                            || (long) value > Integer.MAX_VALUE
                                    && f.kind() == AdminResource.Kind.INTEGER)
                        throw new IllegalArgumentException(f.label() + " is out of range.");
                    if (f.kind() == AdminResource.Kind.RELATION) {
                        String table =
                                "content".equals(f.reference())
                                        ? ("post".equals(input.get("content_type"))
                                                ? "posts"
                                                : "issues")
                                        : f.reference();
                        if (db.queryForList(
                                        "SELECT pkid FROM "
                                                + AdminResource.table(table).table
                                                + " WHERE pkid=? FOR KEY SHARE",
                                        value)
                                .isEmpty())
                            throw new IllegalArgumentException(f.label() + " no longer exists.");
                    }
                }
                case BOOLEAN -> {
                    if (!(value instanceof Boolean))
                        throw new IllegalArgumentException(f.label() + " must be yes or no.");
                }
                case CHOICE -> {
                    if (!f.choices().contains(value.toString()))
                        throw new IllegalArgumentException(
                                "Choose a valid " + f.label().toLowerCase() + ".");
                }
                case DATETIME -> {
                    try {
                        value =
                                Timestamp.from(
                                        value instanceof LocalDateTime d
                                                ? d.toInstant(ZoneOffset.UTC)
                                                : value instanceof Timestamp t
                                                        ? t.toInstant()
                                                        : Instant.parse(value.toString()));
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Enter a valid UTC date and time.");
                    }
                }
                default -> {
                    if (value.toString().length() > f.maxLength())
                        throw new IllegalArgumentException(
                                f.label() + " must be at most " + f.maxLength() + " characters.");
                }
            }
            out.put(f.key(), value);
        }
        if (r == AdminResource.USERS) {
            String email = out.get("email").toString().toLowerCase(Locale.ROOT);
            try {
                new jakarta.mail.internet.InternetAddress(email, true).validate();
            } catch (Exception e) {
                throw new IllegalArgumentException("Enter a valid email address.");
            }
            out.put("email", email);
            if (!out.get("username").toString().matches("[a-zA-Z0-9_]+"))
                throw new IllegalArgumentException(
                        "Use letters, numbers and underscores in the username.");
            String password = Objects.toString(out.get("password"), "");
            if (create || !password.isBlank()) {
                if (!password.matches("(?s)^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,128}$"))
                    throw new IllegalArgumentException(
                            "Use 8–128 characters with an uppercase letter, lowercase letter and"
                                    + " number for the password.");
                if (!password.equals(input.get("password_confirmation")))
                    throw new IllegalArgumentException("The passwords do not match.");
            }
            if (!access.actor().superuser()) {
                if (create
                        && (Boolean.TRUE.equals(out.get("is_staff"))
                                || Boolean.TRUE.equals(out.get("is_superuser"))))
                    throw new AccessDeniedException("Only superusers may grant staff access.");
                if (input.containsKey("groups") || input.containsKey("permissions"))
                    throw new AccessDeniedException("Only superusers may manage permissions.");
                if (create) {
                    out.put("is_staff", false);
                    out.put("is_superuser", false);
                }
            }
        }
        if (r == AdminResource.PROFILES) {
            String country = out.get("country_of_origin").toString().toUpperCase(Locale.ROOT);
            if (!Set.of(Locale.getISOCountries()).contains(country))
                throw new IllegalArgumentException(
                        "Choose a valid two-letter country code, such as IN.");
            out.put("country_of_origin", country);
            if (!out.get("phone_number").toString().matches("\\+?[0-9 ()-]{6,30}"))
                throw new IllegalArgumentException("Enter a valid phone number.");
        }
        if (r == AdminResource.RATINGS) {
            if (number(out.get("rating")) < 1 || number(out.get("rating")) > 5)
                throw new IllegalArgumentException("Rating must be between 1 and 5.");
            long rated = number(out.get("rated_user_pkid")),
                    rater = number(out.get("rating_user_pkid"));
            if (rated == rater)
                throw new IllegalArgumentException("A person cannot rate themselves.");
            // Residents rate technicians on their work, as on the site.
            String raterOccupation = occupationOf(rater), ratedOccupation = occupationOf(rated);
            if (!"tenant".equals(raterOccupation)
                    || ratedOccupation == null
                    || "tenant".equals(ratedOccupation))
                throw new IllegalArgumentException(
                        "A rating must be from a resident to a technician.");
        }
        if (r == AdminResource.REPORTS
                && Objects.equals(out.get("reported_by_pkid"), out.get("reported_user_pkid")))
            throw new IllegalArgumentException("A person cannot report themselves.");
        if (r == AdminResource.CONTENT_VIEWS && out.get("viewer_ip") != null) {
            String ip = out.get("viewer_ip").toString();
            try {
                if (!ip.matches("[0-9a-fA-F:.]+")
                        || (!ip.contains(":") && !ip.matches("\\d{1,3}(\\.\\d{1,3}){3}")))
                    throw new IllegalArgumentException();
                InetAddress.getByName(ip);
            } catch (Exception e) {
                throw new IllegalArgumentException("Enter a valid IPv4 or IPv6 address.");
            }
        }
        return out;
    }

    private void replaceGroups(long user, Object raw) {
        var ids = collection(raw).stream().map(AdminRecordService::number).distinct().toList();
        db.update("DELETE FROM admin_user_groups WHERE user_pkid=?", user);
        for (long group : ids)
            db.update(
                    "INSERT INTO admin_user_groups(user_pkid,group_pkid) VALUES(?,?)", user, group);
    }

    private void replacePermissions(String table, String key, long id, Object raw) {
        var values = collection(raw).stream().map(Object::toString).distinct().toList();
        if (!AdminPermissions.all().containsAll(values))
            throw new IllegalArgumentException("Unknown permission.");
        db.update("DELETE FROM " + table + " WHERE " + key + "=?", id);
        for (String permission : values)
            db.update(
                    "INSERT INTO " + table + "(" + key + ",permission) VALUES(?,?)",
                    id,
                    permission);
    }

    private static Collection<?> collection(Object raw) {
        if (raw == null) return List.of();
        if (raw instanceof Collection<?> c) return c;
        throw new IllegalArgumentException("Choose values from the list.");
    }

    private void saveTags(long post, String input) {
        // Tags match case-insensitively, as in the community API, so "Plumbing" reuses "plumbing".
        Set<String> seen = new HashSet<>();
        var tags =
                Arrays.stream(input.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .filter(s -> seen.add(s.toLowerCase(Locale.ROOT)))
                        .toList();
        if (tags.size() > 20 || tags.stream().anyMatch(t -> t.length() > 100))
            throw new IllegalArgumentException(
                    "Use at most 20 tags, each no longer than 100 characters.");
        db.update("DELETE FROM post_tags WHERE post_pkid=?", post);
        for (String tag : tags) {
            Long id =
                    db.query(
                            "SELECT pkid FROM tags WHERE LOWER(name)=LOWER(?) ORDER BY pkid LIMIT 1",
                            rs -> rs.next() ? rs.getLong(1) : null,
                            tag);
            if (id == null) {
                String slug = Slugs.of(tag);
                if (slug.isBlank()) slug = "tag";
                // Native upsert also handles a tag concurrently added through the community API.
                id =
                        db.queryForObject(
                                "INSERT INTO tags(name,slug) VALUES(?,?) ON CONFLICT(name) DO"
                                        + " UPDATE SET name=EXCLUDED.name RETURNING pkid",
                                Long.class,
                                tag,
                                slug.substring(0, Math.min(55, slug.length()))
                                        + "-"
                                        + UUID.randomUUID());
            }
            db.update("INSERT INTO post_tags(post_pkid,tag_pkid) VALUES(?,?)", post, id);
        }
    }

    private String uniqueSlug(String table, String value) {
        String base = Slugs.of(value);
        if (base.isBlank()) base = "record";
        base = base.substring(0, Math.min(205, base.length()));
        return base + "-" + UUID.randomUUID().toString().substring(0, 12);
    }

    void lockUsers(Set<Long> ids) {
        for (long id : new TreeSet<>(ids))
            db.queryForList("SELECT pkid FROM users WHERE pkid=? FOR UPDATE", id);
    }

    Map<Long, Integer> reportCounts(Set<Long> ids) {
        Map<Long, Integer> counts = new HashMap<>();
        for (long id : ids)
            counts.put(
                    id,
                    db.queryForObject(
                            "SELECT COUNT(DISTINCT reported_by_pkid) FROM reports WHERE"
                                + " reported_user_pkid=? AND reported_by_pkid<>reported_user_pkid"
                                + " AND reported_by_pkid IN (SELECT pkid FROM users WHERE NOT is_guest)",
                            Integer.class,
                            id));
        return counts;
    }

    void recalculateReports(Set<Long> ids, Map<Long, Integer> before) {
        for (long id : ids) {
            int count = reportCounts(Set.of(id)).get(id);
            db.update(
                    "UPDATE profiles SET"
                        + " report_count=?,reputation=GREATEST(0,100-?*20),updated_at=clock_timestamp()"
                        + " WHERE user_pkid=?",
                    count,
                    count,
                    id);
            var rows =
                    db.queryForList(
                            "SELECT email,first_name,is_active,is_staff,is_superuser FROM users WHERE"
                                    + " pkid=?",
                            id);
            if (rows.isEmpty()) continue;
            var user = rows.getFirst();
            if (count >= 5
                    && Boolean.TRUE.equals(user.get("is_active"))
                    && !Profile.keptOnByStaffRole(
                            Boolean.TRUE.equals(user.get("is_staff")),
                            Boolean.TRUE.equals(user.get("is_superuser")))) {
                db.update(
                        "UPDATE users SET"
                            + " is_active=FALSE,auth_version=auth_version+1,updated_at=clock_timestamp()"
                            + " WHERE pkid=?",
                        id);
                send(
                        user.get("email").toString(),
                        "Account Deactivation and Eviction Notice! : " + user.get("first_name"),
                        "Your account on "
                                + mail.siteName()
                                + " has been deactivated after reports from 5 different community"
                                + " members.");
            } else if (before.containsKey(id) && before.get(id) == 0 && count > 0) {
                send(
                        user.get("email").toString(),
                        "Warning: " + user.get("first_name") + " You have been reported!",
                        "A report has been filed against your account on " + mail.siteName() + ".");
            }
            // Removing reports restores the counts, but does not silently reactivate an account.
        }
    }

    private void notifyIssue(Map<String, Object> before, Map<String, Object> after) {
        String email =
                db.queryForObject(
                        "SELECT email FROM users WHERE pkid=?",
                        String.class,
                        after.get("reported_by_pkid"));
        if (before == null)
            send(
                    email,
                    "Issue Report Confirmation",
                    "Your issue \""
                            + after.get("title")
                            + "\" has been logged on "
                            + mail.siteName()
                            + ".");
        if ("resolved".equals(after.get("status"))
                && (before == null || !"resolved".equals(before.get("status"))))
            send(
                    email,
                    "Issue Resolved",
                    "Your issue \""
                            + after.get("title")
                            + "\" has been resolved on "
                            + mail.siteName()
                            + ".");
    }

    private void send(String to, String subject, String body) {
        AfterCommit.run(() -> mail.send(to, subject, body));
    }

    private String occupationOf(long user) {
        return db.queryForList("SELECT occupation FROM profiles WHERE user_pkid=?", String.class, user)
                .stream()
                .findFirst()
                .orElse(null);
    }

    static long number(Object value) {
        return value instanceof Number n ? n.longValue() : Long.parseLong(value.toString());
    }
}
