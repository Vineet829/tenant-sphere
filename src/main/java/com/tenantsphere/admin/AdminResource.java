package com.tenantsphere.admin;

import java.util.Arrays;
import java.util.List;

/** The sole allowlist for admin SQL identifiers and editable fields. */
public enum AdminResource {
    USERS(
            "users",
            "Users",
            "user",
            "username",
            List.of(
                    text("email", "Email", 254),
                    text("username", "Username", 60),
                    text("first_name", "First name", 60),
                    text("last_name", "Last name", 60),
                    new Field("password", "Password", Kind.PASSWORD, false, 128, List.of(), null),
                    bool("is_active", "Active"),
                    bool("is_staff", "Staff status"),
                    bool("is_superuser", "Superuser status"))),
    PROFILES(
            "profiles",
            "Profiles",
            "profile",
            "slug",
            List.of(
                    ref("user_pkid", "User", "users", true),
                    choice("gender", "Gender", "male", "female", "other"),
                    choice(
                            "occupation",
                            "Occupation",
                            "tenant",
                            "mason",
                            "carpenter",
                            "plumber",
                            "roofer",
                            "painter",
                            "electrician",
                            "hvac"),
                    area("bio", "Bio", false),
                    text("phone_number", "Phone number", 30),
                    text("country_of_origin", "Country code", 2),
                    text("city_of_origin", "City", 180))),
    APARTMENTS(
            "apartments",
            "Apartments",
            "apartment",
            "unit_number",
            List.of(
                    text("unit_number", "Unit number", 10), text("building", "Building", 50),
                    integer("floor", "Floor"), ref("tenant_pkid", "Tenant", "users", false))),
    ISSUES(
            "issues",
            "Issues",
            "issue",
            "title",
            List.of(
                    text("title", "Title", 255),
                    area("description", "Description", true),
                    ref("apartment_pkid", "Apartment", "apartments", true),
                    ref("reported_by_pkid", "Reported by", "users", true),
                    ref("assigned_to_pkid", "Assigned to", "users", false),
                    choice("status", "Status", "reported", "in_progress", "resolved"),
                    choice("priority", "Priority", "low", "medium", "high"))),
    POSTS(
            "posts",
            "Posts",
            "post",
            "title",
            List.of(
                    text("title", "Title", 250),
                    area("body", "Body", true),
                    ref("author_pkid", "Author", "users", true),
                    new Field(
                            "tags",
                            "Tags (comma separated)",
                            Kind.TEXT,
                            false,
                            2100,
                            List.of(),
                            null))),
    RATINGS(
            "ratings",
            "Ratings",
            "rating",
            "rating",
            List.of(
                    ref("rated_user_pkid", "Rated user", "users", true),
                            ref("rating_user_pkid", "Rating user", "users", true),
                    integer("rating", "Rating"), area("comment", "Comment", false))),
    REPORTS(
            "reports",
            "Reports",
            "report",
            "title",
            List.of(
                    text("title", "Title", 255), area("description", "Description", true),
                    ref("reported_by_pkid", "Reported by", "users", true),
                            ref("reported_user_pkid", "Reported user", "users", true))),
    CONTENT_VIEWS(
            "content_views",
            "Content views",
            "content view",
            "content_type",
            List.of(
                    choice("content_type", "Content type", "post", "issue"),
                    new Field(
                            "object_pkid", "Content", Kind.RELATION, true, 0, List.of(), "content"),
                    ref("user_pkid", "Viewer", "users", false),
                    new Field("viewer_ip", "Viewer IP", Kind.TEXT, false, 45, List.of(), null),
                    new Field(
                            "last_viewed",
                            "Last viewed (UTC)",
                            Kind.DATETIME,
                            true,
                            0,
                            List.of(),
                            null))),
    TAGS("tags", "Tags", "tag", "name", List.of(text("name", "Name", 100))),
    GROUPS("admin_groups", "Groups", "group", "name", List.of(text("name", "Name", 150)));

    public enum Kind {
        TEXT,
        AREA,
        INTEGER,
        BOOLEAN,
        CHOICE,
        RELATION,
        PASSWORD,
        DATETIME
    }

    public record Field(
            String key,
            String label,
            Kind kind,
            boolean required,
            int maxLength,
            List<String> choices,
            String reference)
            implements java.io.Serializable {}

    public final String table, title, singular, labelColumn;
    public final List<Field> fields;

    AdminResource(String table, String title, String singular, String label, List<Field> fields) {
        this.table = table;
        this.title = title;
        this.singular = singular;
        this.labelColumn = label;
        this.fields = fields;
    }

    public String route() {
        return this == GROUPS ? "groups" : table.replace('_', '-');
    }

    public static AdminResource table(String table) {
        return Arrays.stream(values()).filter(r -> r.table.equals(table)).findFirst().orElseThrow();
    }

    private static Field text(String key, String label, int max) {
        return new Field(key, label, Kind.TEXT, true, max, List.of(), null);
    }

    private static Field area(String key, String label, boolean required) {
        return new Field(key, label, Kind.AREA, required, 100000, List.of(), null);
    }

    private static Field integer(String key, String label) {
        return new Field(key, label, Kind.INTEGER, true, 0, List.of(), null);
    }

    private static Field bool(String key, String label) {
        return new Field(key, label, Kind.BOOLEAN, false, 0, List.of(), null);
    }

    private static Field choice(String key, String label, String... choices) {
        return new Field(key, label, Kind.CHOICE, true, 0, List.of(choices), null);
    }

    private static Field ref(String key, String label, String table, boolean required) {
        return new Field(key, label, Kind.RELATION, required, 0, List.of(), table);
    }
}
