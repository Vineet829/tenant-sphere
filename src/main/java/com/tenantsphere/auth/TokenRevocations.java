package com.tenantsphere.auth;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Sign-in tokens signed out before they run out. Signing out only cleared the browser's cookies, so
 * a copy of a token kept working, and a refresh token kept making new ones. A signed-out token is
 * refused until it would have run out anyway, and so is every other token of the same sign-in, by its
 * session id: copies, and refresh tokens from before the browser's latest refresh. Both kinds of id
 * are kept in revoked_tokens. A token made before session ids were is refused by its own id only.
 */
@Service
public class TokenRevocations {
    private final JdbcTemplate db;
    private final JwtService jwtService;

    public TokenRevocations(JdbcTemplate db, JwtService jwtService) {
        this.db = db;
        this.jwtService = jwtService;
    }

    public void revoke(String token) {
        if (token == null || token.isBlank()) return;
        jwtService.issued(token).ifPresent(issued -> {
            refuse(issued.id(), issued.expiresAt());
            // None of the sign-in's tokens outlives a refresh token made just now.
            if (issued.session() != null) refuse(issued.session(), Instant.now().plus(jwtService.refreshTokenTtl()));
        });
    }

    public boolean isRevoked(String token) {
        return jwtService.issued(token)
                .map(issued -> Boolean.TRUE.equals(db.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM revoked_tokens WHERE token_id IN (?,?))", Boolean.class,
                        issued.id(), issued.session() == null ? issued.id() : issued.session())))
                .orElse(false);
    }

    /** For the cleanup job: tokens and sign-ins that have run out by now. */
    public int removeExpired() {
        return db.update("DELETE FROM revoked_tokens WHERE expires_at < NOW()");
    }

    private void refuse(UUID id, Instant until) {
        db.update("""
                INSERT INTO revoked_tokens(token_id,expires_at) VALUES(?,?)
                ON CONFLICT (token_id) DO UPDATE SET expires_at=GREATEST(revoked_tokens.expires_at, EXCLUDED.expires_at)
                """, id, Timestamp.from(until));
    }
}
