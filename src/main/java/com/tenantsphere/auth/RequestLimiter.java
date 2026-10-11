package com.tenantsphere.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Counts requests in the database, so every instance shares the counts, and refuses those past a
 * limit until its window has passed. Called outside the request's own transaction, so a count stays
 * even when the request then fails, as a wrong password does. A sign-up counts its email inside its
 * own transaction instead, once its checks have passed.
 */
@Service
public class RequestLimiter {
    private final JdbcTemplate db;

    public RequestLimiter(JdbcTemplate db) {
        this.db = db;
    }

    public void consume(RequestLimit limit, String key) {
        long seconds = limit.window.toSeconds();
        Integer attempts = db.queryForObject("""
                INSERT INTO request_limits(key_hash,window_start,attempts) VALUES(?,NOW(),1)
                ON CONFLICT(key_hash) DO UPDATE SET
                    attempts=CASE WHEN request_limits.window_start <= NOW() - make_interval(secs => ?)
                        THEN 1 ELSE request_limits.attempts+1 END,
                    window_start=CASE WHEN request_limits.window_start <= NOW() - make_interval(secs => ?)
                        THEN NOW() ELSE request_limits.window_start END
                RETURNING attempts
                """, Integer.class, hash(limit, key), seconds, seconds);
        if (attempts != null && attempts > limit.most)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, limit.refusal);
    }

    /**
     * An activation, password reset or sign-up email about to be sent: counted per network address, per
     * recipient and in all, so the mailbox they are sent from stays within its provider's sending limits.
     */
    public void consumeAccountEmail(String address, String email) {
        consume(RequestLimit.ACCOUNT_EMAIL_ADDRESS, address);
        consume(RequestLimit.ACCOUNT_EMAIL_RECIPIENT, email);
        consume(RequestLimit.ACCOUNT_EMAIL_ALL, "all");
    }

    /** For the cleanup job: windows that ended a day ago or more. */
    public int removeExpired() {
        return db.update("DELETE FROM request_limits WHERE window_start < NOW() - INTERVAL '1 day'");
    }

    private static String hash(RequestLimit limit, String key) {
        try {
            String value = limit.name() + ":" + String.valueOf(key).trim().toLowerCase(Locale.ROOT);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
