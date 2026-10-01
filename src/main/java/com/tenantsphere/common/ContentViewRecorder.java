package com.tenantsphere.common;

import com.tenantsphere.user.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentViewRecorder {

    private final JdbcTemplate database;

    public ContentViewRecorder(JdbcTemplate database) {
        this.database = database;
    }

    @Transactional
    public void record(String contentType, Long objectPkid, User user, HttpServletRequest request) {
        if (user == null) return;
        // Keep deletion from racing this insert. A stale page view after deletion is ignored.
        String targetQuery = switch (contentType) {
            case "post" -> "SELECT pkid FROM posts WHERE pkid = ? FOR KEY SHARE";
            case "issue" -> "SELECT pkid FROM issues WHERE pkid = ? FOR KEY SHARE";
            default -> throw new IllegalArgumentException("Unsupported content type");
        };
        if (database.queryForList(targetQuery, Long.class, objectPkid).isEmpty()) return;
        String ip = request.getRemoteAddr() == null ? "" : request.getRemoteAddr();
        database.update("""
                INSERT INTO content_views (id, content_type, object_pkid, user_pkid, viewer_ip, last_viewed)
                VALUES (?, ?, ?, ?, ?, NOW())
                ON CONFLICT ON CONSTRAINT uq_content_views
                DO UPDATE SET last_viewed=NOW(), updated_at=NOW()
                """, java.util.UUID.randomUUID(), contentType, objectPkid, user.getPkid(), ip);
    }

}
