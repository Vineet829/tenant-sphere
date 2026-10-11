package com.tenantsphere.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

// V10 runs over accounts as they were before it: each one that was ever on is
// remembered as activated, and only one that never was still waits for its
// activation email.
class FirstActivationMigrationTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static final Instant JOINED = Instant.parse("2025-03-01T09:00:00Z");
    private static final Instant SIGNED_IN = Instant.parse("2025-03-02T10:00:00Z");

    private static DriverManagerDataSource dataSource;
    private static JdbcTemplate db;

    @BeforeAll
    static void start() {
        POSTGRES.start();
        dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        db = new JdbcTemplate(dataSource);
    }

    @AfterAll
    static void stop() {
        POSTGRES.stop();
    }

    @Test
    void accountsThatWereEverOnAreRememberedAsActivated() {
        migrateTo("9");
        long waiting = user("waiting", false, null);
        long active = user("active", true, null);
        long signedInThenOff = user("off_after_sign_in", false, SIGNED_IN);
        long offInConsole = user("off_in_console", false, null);
        db.update(
                "INSERT INTO admin_audit_log (actor_label, resource, object_pkid, object_label, action, before_data,"
                        + " after_data) VALUES ('admin@example.invalid', 'users', ?, 'off_in_console', 'change',"
                        + " '{\"is_active\": true}'::jsonb, '{\"is_active\": false}'::jsonb)",
                offInConsole);

        migrateTo("10");

        assertThat(activatedAt(waiting)).isNull();
        assertThat(activatedAt(active)).isEqualTo(JOINED);
        assertThat(activatedAt(signedInThenOff)).isEqualTo(SIGNED_IN);
        assertThat(activatedAt(offInConsole)).isEqualTo(JOINED);
    }

    private static void migrateTo(String version) {
        Flyway.configure().dataSource(dataSource).target(version).load().migrate();
    }

    private static long user(String name, boolean active, Instant lastLogin) {
        return db.queryForObject(
                "INSERT INTO users (id, email, username, first_name, last_name, password, is_active, date_joined,"
                        + " last_login) VALUES (?, ?, ?, 'QA', 'Resident', 'unused', ?, ?, ?) RETURNING pkid",
                Long.class,
                UUID.randomUUID(),
                name + "@example.invalid",
                name,
                active,
                Timestamp.from(JOINED),
                lastLogin == null ? null : Timestamp.from(lastLogin));
    }

    private static Instant activatedAt(long pkid) {
        Instant value = db.queryForObject("SELECT activated_at FROM users WHERE pkid = ?", Instant.class, pkid);
        return value == null ? null : value.truncatedTo(ChronoUnit.MICROS);
    }
}
