package com.tenantsphere.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = "spring.jpa.show-sql=false")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RequestLimiter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RequestLimiterTest {
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

    @Autowired private RequestLimiter limiter;
    @Autowired private JdbcTemplate db;

    @BeforeEach
    void clear() {
        db.execute("TRUNCATE request_limits");
    }

    @Test
    void refusesPastTheLimitUntilTheWindowHasPassed() {
        for (int i = 0; i < 5; i++) limiter.consume(RequestLimit.SIGN_UP_ADDRESS, "203.0.113.7");
        assertThatThrownBy(() -> limiter.consume(RequestLimit.SIGN_UP_ADDRESS, "203.0.113.7"))
                .isInstanceOfSatisfying(ResponseStatusException.class, refused -> {
                    assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(refused.getReason())
                            .isEqualTo("Too many sign-ups from this network. Please try again later.");
                });
        // Another address, and another kind of request from this one, count on their own.
        limiter.consume(RequestLimit.SIGN_UP_ADDRESS, "203.0.113.8");
        limiter.consume(RequestLimit.SIGN_IN_ADDRESS, "203.0.113.7");

        db.update("UPDATE request_limits SET window_start = NOW() - INTERVAL '61 minutes'");
        limiter.consume(RequestLimit.SIGN_UP_ADDRESS, "203.0.113.7");
    }

    @Test
    void emailsCountAlikeWhateverTheirCaseAndAreNeverStored() {
        for (int i = 0; i < 3; i++) limiter.consume(RequestLimit.ACCOUNT_EMAIL_RECIPIENT, "Resident@Example.invalid");
        assertThatThrownBy(() -> limiter.consume(RequestLimit.ACCOUNT_EMAIL_RECIPIENT, "resident@example.invalid "))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(db.queryForList("SELECT key_hash FROM request_limits", String.class))
                .singleElement()
                .satisfies(key -> assertThat(key).doesNotContainIgnoringCase("example").hasSize(64));
    }

    @Test
    void cleanupRemovesWindowsThatEndedLongAgo() {
        limiter.consume(RequestLimit.SIGN_IN_EMAIL, "old@example.invalid");
        limiter.consume(RequestLimit.SIGN_IN_EMAIL, "new@example.invalid");
        db.update("UPDATE request_limits SET window_start = NOW() - INTERVAL '2 days' WHERE key_hash = ("
                + "SELECT key_hash FROM request_limits ORDER BY window_start LIMIT 1)");
        assertThat(limiter.removeExpired()).isOne();
        assertThat(db.queryForObject("SELECT count(*) FROM request_limits", Integer.class)).isOne();
    }
}
