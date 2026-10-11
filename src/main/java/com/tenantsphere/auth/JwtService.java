package com.tenantsphere.auth;

import com.tenantsphere.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.tenantsphere.user.User;
import com.tenantsphere.user.UserRepository;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final UserRepository users;

    public JwtService(AppProperties properties) {
        this(properties, null);
    }

    @Autowired
    public JwtService(AppProperties properties, UserRepository users) {
        this.users = users;
        this.key = Keys.hmacShaKeyFor(
                properties.jwt().signingKey().getBytes(StandardCharsets.UTF_8));
        this.accessTtl = properties.jwt().accessTokenTtl();
        this.refreshTtl = properties.jwt().refreshTokenTtl();
    }

    // Every token of one sign-in carries that sign-in's session id, refreshed ones too, so signing out
    // refuses them all: copies, and refresh tokens from before the latest refresh.
    public String issueAccessToken(UUID userId, UUID session) {
        return issue(userId, session, TokenType.ACCESS, accessTtl);
    }

    public String issueRefreshToken(UUID userId, UUID session) {
        return issue(userId, session, TokenType.REFRESH, refreshTtl);
    }

    /** How long a refresh token lasts, which no token of a sign-in outlives from the moment it was made. */
    public Duration refreshTokenTtl() {
        return refreshTtl;
    }

    private String issue(UUID userId, UUID session, TokenType type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("user_id", userId.toString())
                .claim("token_type", type.name().toLowerCase())
                .claim("sid", session.toString())
                .claim("auth_version", users == null ? 0L : users.findByExternalId(userId).map(User::getAuthVersion).orElse(0L))
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public UUID extractUserId(String token, TokenType expected) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String type = claims.get("token_type", String.class);
        if (!expected.name().toLowerCase().equals(type)) {
            throw new IllegalArgumentException("Unexpected token type: " + type);
        }
        return UUID.fromString(claims.get("user_id", String.class));
    }

    /**
     * A token this server signed and that hasn't run out: its id, its sign-in's session id (none for a
     * token made before session ids were), and when it runs out.
     */
    public Optional<Issued> issued(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String session = claims.get("sid", String.class);
            return Optional.of(new Issued(
                    UUID.fromString(claims.getId()),
                    session == null ? null : UUID.fromString(session),
                    claims.getExpiration().toInstant()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public record Issued(UUID id, UUID session, Instant expiresAt) {}

    /** The session id of the sign-in a token belongs to, if it is a valid token that has one. */
    public Optional<UUID> sessionOf(String token) {
        return issued(token).map(Issued::session);
    }

    public boolean isCurrent(String token, User user) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Number version = claims.get("auth_version", Number.class);
        // Existing sessions remain valid until a password change or deactivation revokes them.
        return (version == null ? 0L : version.longValue()) == user.getAuthVersion();
    }
}
