package com.tenantsphere.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AdminQueryService {
    private static final Set<AdminResource> RELATIONS =
            Set.of(
                    AdminResource.USERS,
                    AdminResource.APARTMENTS,
                    AdminResource.POSTS,
                    AdminResource.ISSUES,
                    AdminResource.GROUPS);

    private final JdbcTemplate db;
    private final AdminPermissions permissions;

    public AdminQueryService(JdbcTemplate db, AdminPermissions permissions) {
        this.db = db;
        this.permissions = permissions;
    }

    public record Query(
            String search,
            Map<String, Object> filters,
            LocalDate from,
            LocalDate to,
            String sort,
            boolean descending) {
        public static Query empty() {
            return new Query("", Map.of(), null, null, "pkid", true);
        }
    }

    public record Choice(Long id, String label) implements java.io.Serializable {
        @Override
        public String toString() {
            return label;
        }
    }

    public List<Map<String, Object>> list(AdminResource r, Query q, int offset, int limit) {
        permissions.require(r, "view");
        List<Object> args = new ArrayList<>();
        String where = where(r, q, args);
        String order = columns(r).contains(q.sort()) ? q.sort() : "pkid";
        args.add(Math.max(0, Math.min(limit, 100)));
        args.add(Math.max(0, offset));
        return db.queryForList(
                "SELECT * FROM ("
                        + select(r)
                        + ") catalog"
                        + where
                        + " ORDER BY "
                        + order
                        + (q.descending() ? " DESC" : " ASC")
                        + " NULLS LAST, pkid DESC LIMIT ? OFFSET ?",
                args.toArray());
    }

    public int count(AdminResource r, Query q) {
        permissions.require(r, "view");
        List<Object> args = new ArrayList<>();
        String where = where(r, q, args);
        return Math.toIntExact(
                db.queryForObject(
                        "SELECT COUNT(*) FROM (" + select(r) + ") catalog" + where,
                        Long.class,
                        args.toArray()));
    }

    public Map<String, Object> read(AdminResource r, long id) {
        permissions.require(r, "view");
        return readInternal(r, id);
    }

    Map<String, Object> readInternal(AdminResource r, long id) {
        var rows = db.queryForList(select(r) + " WHERE r.pkid=?", id);
        if (rows.isEmpty())
            throw new IllegalArgumentException("This record no longer exists. Refresh the list.");
        Map<String, Object> row = new LinkedHashMap<>(rows.getFirst());
        if (r == AdminResource.USERS && permissions.actor().superuser()) {
            row.put(
                    "groups",
                    db.queryForList(
                            "SELECT group_pkid FROM admin_user_groups WHERE user_pkid=? ORDER BY"
                                    + " group_pkid",
                            Long.class,
                            id));
            row.put(
                    "permissions",
                    db.queryForList(
                            "SELECT permission FROM admin_user_permissions WHERE user_pkid=? ORDER"
                                    + " BY permission",
                            String.class,
                            id));
        }
        if (r == AdminResource.GROUPS)
            row.put(
                    "permissions",
                    db.queryForList(
                            "SELECT permission FROM admin_group_permissions WHERE group_pkid=?"
                                    + " ORDER BY permission",
                            String.class,
                            id));
        return row;
    }

    public List<Choice> choices(String table, String search, int offset, int limit) {
        AdminResource r = relation(table);
        if (r == AdminResource.GROUPS) permissions.requireSuperuser();
        // Labels carry names and emails, so searching a relation needs view access to its table.
        permissions.require(r, "view");
        String label = choiceLabel(r);
        return db.query(
                "SELECT pkid,"
                        + label
                        + " AS label FROM "
                        + r.table
                        + " WHERE LOWER("
                        + label
                        + ") LIKE ? ESCAPE '!' ORDER BY "
                        + r.labelColumn
                        + ",pkid LIMIT ? OFFSET ?",
                (rs, n) -> new Choice(rs.getLong("pkid"), rs.getString("label")),
                pattern(search),
                Math.min(100, Math.max(1, limit)),
                Math.max(0, offset));
    }

    public Choice choice(String table, long id) {
        AdminResource r = relation(table);
        if (r == AdminResource.GROUPS) permissions.requireSuperuser();
        // An editor may show a saved relation it cannot view; identify it without exposing the label.
        if (!permissions.allowed(r, "view")) return new Choice(id, "#" + id);
        var rows =
                db.query(
                        "SELECT pkid,"
                                + choiceLabel(r)
                                + " AS label FROM "
                                + r.table
                                + " WHERE pkid=?",
                        (rs, n) -> new Choice(rs.getLong(1), rs.getString(2)),
                        id);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public List<Map<String, Object>> contentViews(AdminResource r, long id) {
        permissions.require(r, "view");
        permissions.require(AdminResource.CONTENT_VIEWS, "view");
        return list(
                AdminResource.CONTENT_VIEWS,
                new Query(
                        "",
                        Map.of(
                                "content_type",
                                r == AdminResource.POSTS ? "post" : "issue",
                                "object_pkid",
                                id),
                        null,
                        null,
                        "last_viewed",
                        true),
                0,
                100);
    }

    public static List<String> columns(AdminResource r) {
        return switch (r) {
            case USERS ->
                    List.of(
                            "pkid",
                            "username",
                            "email",
                            "first_name",
                            "last_name",
                            "is_active",
                            "is_staff",
                            "is_superuser",
                            "date_joined",
                            "last_login");
            case PROFILES ->
                    List.of(
                            "pkid",
                            "user_label",
                            "gender",
                            "occupation",
                            "phone_number",
                            "country_of_origin",
                            "city_of_origin",
                            "report_count",
                            "reputation",
                            "slug");
            case APARTMENTS -> List.of("pkid", "unit_number", "building", "floor", "tenant_label");
            case ISSUES ->
                    List.of(
                            "pkid",
                            "title",
                            "apartment_label",
                            "reported_by_label",
                            "assigned_to_label",
                            "status",
                            "priority",
                            "resolved_on",
                            "total_views",
                            "created_at");
            case POSTS ->
                    List.of(
                            "pkid",
                            "title",
                            "author_label",
                            "tags",
                            "upvotes",
                            "downvotes",
                            "total_views",
                            "created_at");
            case RATINGS ->
                    List.of(
                            "pkid",
                            "rated_user_label",
                            "rating_user_label",
                            "rating",
                            "comment",
                            "average_rating",
                            "created_at");
            case REPORTS ->
                    List.of(
                            "pkid",
                            "title",
                            "reported_by_label",
                            "reported_user_label",
                            "report_count",
                            "created_at");
            case CONTENT_VIEWS ->
                    List.of(
                            "pkid",
                            "content_type",
                            "content_label",
                            "user_label",
                            "viewer_ip",
                            "last_viewed",
                            "created_at");
            case TAGS -> List.of("pkid", "name", "slug", "post_count", "created_at");
            case GROUPS -> List.of("pkid", "name", "members", "created_at");
        };
    }

    private String where(AdminResource r, Query q, List<Object> args) {
        StringBuilder out = new StringBuilder(" WHERE TRUE");
        if (q.search() != null && !q.search().isBlank()) {
            out.append(" AND (");
            List<String> searchable = new ArrayList<>(columns(r));
            for (var field : r.fields)
                if ((field.kind() == AdminResource.Kind.AREA) && !searchable.contains(field.key()))
                    searchable.add(field.key());
            out.append(
                    String.join(
                            " OR ",
                            searchable.stream()
                                    .map(
                                            c ->
                                                    "LOWER(COALESCE("
                                                            + c
                                                            + "::text,'')) LIKE ? ESCAPE '!'")
                                    .toList()));
            out.append(")");
            for (String unused : searchable) args.add(pattern(q.search()));
        }
        for (var filter : q.filters().entrySet()) {
            if (!r.fields.stream()
                    .anyMatch(
                            f ->
                                    f.key().equals(filter.getKey())
                                            && f.kind() != AdminResource.Kind.PASSWORD))
                throw new IllegalArgumentException("Invalid filter.");
            if (filter.getValue() != null && !filter.getValue().toString().isBlank()) {
                boolean booleanField = r.fields.stream().anyMatch(f -> f.key().equals(filter.getKey())
                        && f.kind() == AdminResource.Kind.BOOLEAN);
                if (booleanField) {
                    String value = filter.getValue().toString();
                    if (!value.equals("true") && !value.equals("false")) throw new IllegalArgumentException("Invalid boolean filter.");
                    out.append(" AND ").append(filter.getKey()).append(" = ?");
                    args.add(Boolean.valueOf(value));
                } else {
                    out.append(" AND ").append(filter.getKey()).append("::text = ?");
                    args.add(filter.getValue().toString());
                }
            }
        }
        if (q.from() != null) {
            out.append(" AND created_at >= ?::date");
            args.add(q.from().toString());
        }
        if (q.to() != null) {
            out.append(" AND created_at < (?::date + INTERVAL '1 day')");
            args.add(q.to().toString());
        }
        return out.toString();
    }

    private AdminResource relation(String table) {
        permissions.actor();
        AdminResource r = AdminResource.table(table);
        if (!RELATIONS.contains(r)) throw new IllegalArgumentException("Invalid related record.");
        return r;
    }

    static String pattern(String text) {
        return "%"
                + (text == null
                        ? ""
                        : text.toLowerCase(Locale.ROOT)
                                .replace("!", "!!")
                                .replace("%", "!%")
                                .replace("_", "!_"))
                + "%";
    }

    private static String choiceLabel(AdminResource r) {
        return switch (r) {
            case USERS ->
                    "username || ' · ' || first_name || ' ' || last_name || ' (' || email || ')'";
            case APARTMENTS -> "unit_number || ' · ' || building || ' / Floor ' || floor";
            default -> r.labelColumn + "::text";
        };
    }

    private static String userLabel(String column) {
        return "(SELECT username || ' · ' || first_name || ' ' || last_name FROM users u WHERE"
                + " u.pkid=r."
                + column
                + ")";
    }

    private static String select(AdminResource r) {
        String base =
                r == AdminResource.USERS
                        ? "r.pkid,r.id,r.email,r.username,r.first_name,r.last_name,r.is_active,r.is_staff,r.is_superuser,r.date_joined,r.last_login,r.created_at,r.updated_at"
                        : "r.*";
        String extra =
                switch (r) {
                    case USERS -> "";
                    case PROFILES -> "," + userLabel("user_pkid") + " AS user_label";
                    case APARTMENTS -> "," + userLabel("tenant_pkid") + " AS tenant_label";
                    case ISSUES ->
                            ","
                                    + userLabel("reported_by_pkid")
                                    + " AS reported_by_label,"
                                    + userLabel("assigned_to_pkid")
                                    + " AS assigned_to_label,(SELECT unit_number FROM apartments a"
                                    + " WHERE a.pkid=r.apartment_pkid) AS apartment_label,(SELECT"
                                    + " count(*) FROM content_views v WHERE v.content_type='issue'"
                                    + " AND v.object_pkid=r.pkid) AS total_views";
                    case POSTS ->
                            ","
                                    + userLabel("author_pkid")
                                    + " AS author_label,(SELECT string_agg(t.name, ', ' ORDER BY"
                                    + " t.name) FROM post_tags pt JOIN tags t ON t.pkid=pt.tag_pkid"
                                    + " WHERE pt.post_pkid=r.pkid) AS tags,(SELECT count(*) FROM"
                                    + " content_views v WHERE v.content_type='post' AND"
                                    + " v.object_pkid=r.pkid) AS total_views";
                    case RATINGS ->
                            ","
                                    + userLabel("rated_user_pkid")
                                    + " AS rated_user_label,"
                                    + userLabel("rating_user_pkid")
                                    + " AS rating_user_label,(SELECT ROUND(AVG(rating),2) FROM"
                                    + " ratings t WHERE t.rated_user_pkid=r.rated_user_pkid) AS"
                                    + " average_rating";
                    case REPORTS ->
                            ","
                                    + userLabel("reported_by_pkid")
                                    + " AS reported_by_label,"
                                    + userLabel("reported_user_pkid")
                                    + " AS reported_user_label,(SELECT report_count FROM profiles p"
                                    + " WHERE p.user_pkid=r.reported_user_pkid) AS report_count";
                    case CONTENT_VIEWS ->
                            ","
                                    + userLabel("user_pkid")
                                    + " AS user_label,CASE WHEN r.content_type='post' THEN (SELECT"
                                    + " title FROM posts p WHERE p.pkid=r.object_pkid) WHEN"
                                    + " r.content_type='issue' THEN (SELECT title FROM issues i"
                                    + " WHERE i.pkid=r.object_pkid) END AS content_label";
                    case TAGS ->
                            ",(SELECT COUNT(*) FROM post_tags pt WHERE pt.tag_pkid=r.pkid) AS"
                                    + " post_count";
                    case GROUPS ->
                            ",(SELECT COUNT(*) FROM admin_user_groups ug WHERE"
                                    + " ug.group_pkid=r.pkid) AS members";
                };
        return "SELECT "
                + base
                + ",r.updated_at::text AS revision"
                + extra
                + " FROM "
                + r.table
                + " r";
    }
}
