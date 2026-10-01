package com.tenantsphere.admin;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AdminAuditService {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final AdminPermissions permissions;

    public AdminAuditService(JdbcTemplate db, ObjectMapper json, AdminPermissions permissions) {
        this.db = db;
        this.json = json;
        this.permissions = permissions;
    }

    void record(
            AdminResource r,
            long id,
            String action,
            Map<String, Object> before,
            Map<String, Object> after) {
        var actor = permissions.actor();
        var data = after == null ? before : after;
        String label = Objects.toString(data.get(r.labelColumn), r.singular + " #" + id);
        db.update(
                "INSERT INTO"
                    + " admin_audit_log(actor_id,actor_label,resource,object_pkid,object_label,action,before_data,after_data)"
                    + " VALUES(?,?,?,?,?,?,?::jsonb,?::jsonb)",
                actor.id(),
                actor.email(),
                r.table,
                id,
                label,
                action,
                encode(before),
                encode(after));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> history(
            AdminResource r, Long id, String search, int offset, int limit) {
        permissions.actor();
        if (r != null) permissions.require(r, "view");
        List<Object> args = new ArrayList<>();
        List<String> resources =
                Arrays.stream(AdminResource.values())
                        .filter(x -> permissions.allowed(x, "view"))
                        .map(x -> x.table)
                        .toList();
        if (resources.isEmpty()) return List.of();
        String where =
                "resource IN ("
                        + String.join(",", Collections.nCopies(resources.size(), "?"))
                        + ")";
        args.addAll(resources);
        if (r != null) {
            where += " AND resource=?";
            args.add(r.table);
        }
        if (id != null) {
            where += " AND object_pkid=?";
            args.add(id);
        }
        if (search != null && !search.isBlank()) {
            where += " AND (actor_label ILIKE ? ESCAPE '!' OR object_label ILIKE ? ESCAPE '!')";
            args.add(AdminQueryService.pattern(search));
            args.add(AdminQueryService.pattern(search));
        }
        args.add(Math.max(1, Math.min(100, limit)));
        args.add(Math.max(0, offset));
        return db.queryForList(
                "SELECT"
                    + " pkid,actor_label,resource,object_pkid,object_label,action,before_data::text,after_data::text,created_at"
                    + " FROM admin_audit_log WHERE "
                        + where
                        + " ORDER BY pkid DESC LIMIT ? OFFSET ?",
                args.toArray());
    }

    private String encode(Map<String, Object> data) {
        if (data == null) return null;
        try {
            var safe = new LinkedHashMap<String, Object>();
            data.forEach(
                    (key, value) -> {
                        if (!Set.of("password", "revision").contains(key))
                            safe.put(
                                    key,
                                    value instanceof java.util.Date ? value.toString() : value);
                    });
            return json.writeValueAsString(safe);
        } catch (Exception e) {
            throw new IllegalStateException("Could not record administration history.", e);
        }
    }
}
