package com.tenantsphere.auth;

import com.tenantsphere.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
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

    public String issueAccessToken(UUID userId) {
        return issue(userId, TokenType.ACCESS, accessTtl);
    }

    public String issueRefreshToken(UUID userId) {
        return issue(userId, TokenType.REFRESH, refreshTtl);
    }

    private String issue(UUID userId, TokenType type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("user_id", userId.toString())
                .claim("token_type", type.name().toLowerCase())
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

    public boolean isCurrent(String token, User user) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Number version = claims.get("auth_version", Number.class);
        // Existing sessions remain valid until a password change or deactivation revokes them.
        return (version == null ? 0L : version.longValue()) == user.getAuthVersion();
    }
}
