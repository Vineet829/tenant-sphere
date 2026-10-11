package com.tenantsphere.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

// V14 runs over profiles as they were made before it: each that had no address of its own gets one,
// and the rest keep theirs.
class ReachableProfileSlugsMigrationTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

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
    void profilesWithoutAnAddressGetOne() {
        Flyway.configure().dataSource(dataSource).target("13").load().migrate();
        UUID empty = profile("___", "");
        UUID next = profile("____", "-1");
        UUID console = profile("_____", "-0f3a9c21");
        UUID kept = profile("river_view", "river-view");

        Flyway.configure().dataSource(dataSource).target("14").load().migrate();

        assertThat(slug(empty)).isEqualTo("profile-" + empty.toString().replace("-", ""));
        assertThat(slug(next)).isEqualTo("profile-" + next.toString().replace("-", ""));
        assertThat(slug(console)).isEqualTo("profile-" + console.toString().replace("-", ""));
        assertThat(slug(kept)).isEqualTo("river-view");
    }

    private static UUID profile(String username, String slug) {
        long user = db.queryForObject(
                "INSERT INTO users (id, email, username, first_name, last_name, password) VALUES"
                        + " (?, ?, ?, 'QA', 'Resident', 'unused') RETURNING pkid",
                Long.class, UUID.randomUUID(), username + "@example.invalid", username);
        UUID id = UUID.randomUUID();
        db.update("INSERT INTO profiles (id, user_pkid, slug) VALUES (?, ?, ?)", id, user, slug);
        return id;
    }

    private static String slug(UUID profile) {
        return db.queryForObject("SELECT slug FROM profiles WHERE id = ?", String.class, profile);
    }
}
