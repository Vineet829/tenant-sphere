package com.tenantsphere.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AdminPermissions {
    private final JdbcTemplate db;

    public AdminPermissions(JdbcTemplate db) {
        this.db = db;
    }

    public record Actor(long pkid, UUID id, String email, boolean superuser) {}

    public Actor actor() {
        var principal = AdminAccess.requireStaff();
        // Re-read privileges for every operation; an open Vaadin session is not an authority cache.
        var rows =
                db.queryForList(
                        "SELECT pkid,id,email,is_superuser FROM users WHERE pkid=? AND is_active"
                                + " AND (is_staff OR is_superuser)",
                        principal.getPkid());
        if (rows.isEmpty())
            throw new AccessDeniedException(
                    "Your administration access has been removed. Please sign in again.");
        var row = rows.getFirst();
        return new Actor(
                ((Number) row.get("pkid")).longValue(),
                (UUID) row.get("id"),
                (String) row.get("email"),
                (boolean) row.get("is_superuser"));
    }

    public boolean allowed(AdminResource resource, String action) {
        Actor actor = actor();
        if (actor.superuser()) return true;
        if (resource == AdminResource.GROUPS) return false;
        var grants = grants(actor.pkid());
        return grants.contains(resource.table + "." + action)
                || (action.equals("view") && grants.contains(resource.table + ".change"));
    }

    public Actor require(AdminResource resource, String action) {
        if (!allowed(resource, action))
            throw new AccessDeniedException(
                    "You do not have permission to "
                            + action
                            + " "
                            + resource.title.toLowerCase()
                            + ".");
        return actor();
    }

    public void requireSuperuser() {
        if (!actor().superuser())
            throw new AccessDeniedException(
                    "Only superusers may manage permissions or privileged accounts.");
    }

    public Set<String> grants(long user) {
        return new LinkedHashSet<>(
                db.queryForList(
                        """
                        SELECT permission FROM admin_user_permissions WHERE user_pkid=?
                        UNION SELECT gp.permission FROM admin_group_permissions gp
                        JOIN admin_user_groups ug ON ug.group_pkid=gp.group_pkid WHERE ug.user_pkid=?
                        """,
                        String.class,
                        user,
                        user));
    }

    public static List<String> all() {
        return Arrays.stream(AdminResource.values())
                .filter(r -> r != AdminResource.GROUPS)
                .flatMap(
                        r ->
                                List.of("view", "add", "change", "delete").stream()
                                        .map(a -> r.table + "." + a))
                .toList();
    }
}
