package com.tenantsphere.auth;

import com.tenantsphere.profile.AvatarStorage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestCleanup {
    private final JdbcTemplate db;
    private final AvatarStorage avatars;
    public GuestCleanup(JdbcTemplate db, AvatarStorage avatars) { this.db = db; this.avatars = avatars; }

    @Scheduled(fixedDelayString = "${app.guest.cleanup-delay:600000}")
    @Transactional
    public void removeExpired() {
        db.queryForObject("SELECT pg_advisory_xact_lock(170628,8)", Object.class);
        var guests = db.queryForList("SELECT pkid FROM users WHERE is_guest AND guest_expires_at <= NOW() ORDER BY pkid", Long.class);
        for (long guest : guests) {
            var posts = db.queryForList("""
                    SELECT pkid FROM posts WHERE pkid IN (
                        SELECT post_pkid FROM post_upvotes WHERE user_pkid=?
                        UNION SELECT post_pkid FROM post_downvotes WHERE user_pkid=?)
                    ORDER BY pkid FOR UPDATE
                    """, Long.class, guest, guest);
            var photos = db.queryForList("SELECT avatar FROM profiles WHERE user_pkid=?", String.class, guest);
            db.update("DELETE FROM apartments WHERE tenant_pkid=?", guest);
            db.update("DELETE FROM content_views WHERE user_pkid=?", guest);
            db.update("DELETE FROM users WHERE pkid=? AND is_guest AND guest_expires_at <= NOW()", guest);
            for (long post : posts) db.update("""
                    UPDATE posts SET upvotes=(SELECT count(*) FROM post_upvotes WHERE post_pkid=?),
                        downvotes=(SELECT count(*) FROM post_downvotes WHERE post_pkid=?) WHERE pkid=?
                    """, post, post, post);
            photos.forEach(avatars::removeAfterCommit);
        }
        db.update("DELETE FROM guest_login_limits WHERE window_start < NOW() - INTERVAL '1 hour'");
    }
}
