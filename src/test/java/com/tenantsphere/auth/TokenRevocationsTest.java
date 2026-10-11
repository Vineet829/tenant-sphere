package com.tenantsphere.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenantsphere.config.AppProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = "spring.jpa.show-sql=false")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TokenRevocations.class, TokenRevocationsTest.Configuration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TokenRevocationsTest {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @AfterAll
    static void cleanup() {
        POSTGRES.stop();
    }

    @TestConfiguration
    static class Configuration {
        @Bean
        JwtService jwtService() {
            Duration day = Duration.ofDays(1);
            return new JwtService(new AppProperties(new AppProperties.Jwt("k".repeat(64), day, day), null, null, null));
        }
    }

    @Autowired private TokenRevocations revocations;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate db;

    @BeforeEach
    void clear() {
        db.execute("TRUNCATE revoked_tokens");
    }

    @Test
    void aSignedOutTokenIsRefusedAndTheOthersAreNot() {
        UUID user = UUID.randomUUID();
        String signedOut = jwtService.issueRefreshToken(user, UUID.randomUUID());
        String other = jwtService.issueRefreshToken(user, UUID.randomUUID());

        revocations.revoke(signedOut);
        revocations.revoke(signedOut);

        assertThat(revocations.isRevoked(signedOut)).isTrue();
        assertThat(revocations.isRevoked(other)).isFalse();
        assertThat(revocations.isRevoked("not-a-token")).isFalse();
        revocations.revoke("not-a-token");
        // The token's id and its sign-in's.
        assertThat(db.queryForObject("SELECT count(*) FROM revoked_tokens", Integer.class)).isEqualTo(2);
    }

    @Test
    void signingOutRefusesEveryTokenOfThatSignInAndNoOther() {
        UUID user = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        // A refresh token from before the browser's last refresh, then the two the browser holds.
        String older = jwtService.issueRefreshToken(user, session);
        String access = jwtService.issueAccessToken(user, session);
        String refresh = jwtService.issueRefreshToken(user, session);
        String elsewhere = jwtService.issueRefreshToken(user, UUID.randomUUID());

        revocations.revoke(access);
        revocations.revoke(refresh);

        assertThat(revocations.isRevoked(older)).isTrue();
        assertThat(revocations.isRevoked(access)).isTrue();
        assertThat(revocations.isRevoked(refresh)).isTrue();
        assertThat(revocations.isRevoked(elsewhere)).isFalse();
        // The sign-in stays refused as long as a refresh token made at sign-out would last.
        assertThat(db.queryForObject("SELECT expires_at FROM revoked_tokens WHERE token_id = ?",
                        Timestamp.class, session).toInstant())
                .isAfter(Instant.now().plus(Duration.ofHours(23)));
    }

    @Test
    void aTokenFromBeforeSessionIdsIsRefusedByItsOwnId() {
        Instant now = Instant.now();
        String old = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("token_type", "refresh")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofDays(1))))
                .signWith(Keys.hmacShaKeyFor("k".repeat(64).getBytes(StandardCharsets.UTF_8)))
                .compact();

        revocations.revoke(old);

        assertThat(revocations.isRevoked(old)).isTrue();
        assertThat(db.queryForObject("SELECT count(*) FROM revoked_tokens", Integer.class)).isOne();
    }

    @Test
    void tokensThatHaveRunOutAreRemoved() {
        UUID session = UUID.randomUUID();
        revocations.revoke(jwtService.issueAccessToken(UUID.randomUUID(), session));
        revocations.revoke(jwtService.issueAccessToken(UUID.randomUUID(), UUID.randomUUID()));
        db.update("UPDATE revoked_tokens SET expires_at = NOW() - INTERVAL '1 minute' WHERE token_id = ?", session);

        assertThat(revocations.removeExpired()).isOne();
        assertThat(db.queryForObject("SELECT count(*) FROM revoked_tokens", Integer.class)).isEqualTo(3);
    }
}
