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

// V12 runs over posts, tags and reports as they were made before it: each that had no address of
// its own gets one, and the rest keep theirs.
class ReachableSlugsMigrationTest {

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
    void postsTagsAndReportsWithoutAnAddressGetOne() {
        Flyway.configure().dataSource(dataSource).target("11").load().migrate();
        long author = db.queryForObject(
                "INSERT INTO users (id, email, username, first_name, last_name, password) VALUES"
                        + " (?, 'author@example.invalid', 'author', 'QA', 'Resident', 'unused') RETURNING pkid",
                Long.class, UUID.randomUUID());
        long neighbour = db.queryForObject(
                "INSERT INTO users (id, email, username, first_name, last_name, password) VALUES"
                        + " (?, 'neighbour@example.invalid', 'neighbour', 'QA', 'Resident', 'unused') RETURNING pkid",
                Long.class, UUID.randomUUID());
        UUID empty = post("", author);
        UUID second = post("-2", author);
        UUID route = post("my-posts", author);
        UUID kept = post("water-tank", author);
        UUID tag = UUID.randomUUID();
        db.update("INSERT INTO tags (id, name, slug) VALUES (?, 'garden', '')", tag);
        UUID report = UUID.randomUUID();
        db.update("INSERT INTO reports (id, title, slug, reported_by_pkid, reported_user_pkid, description)"
                + " VALUES (?, 'Noise', '', ?, ?, 'Late music')", report, author, neighbour);

        Flyway.configure().dataSource(dataSource).target("12").load().migrate();

        assertThat(slug("posts", empty)).isEqualTo("post-" + empty.toString().replace("-", ""));
        assertThat(slug("posts", second)).isEqualTo("post-" + second.toString().replace("-", ""));
        assertThat(slug("posts", route)).isEqualTo("post-" + route.toString().replace("-", ""));
        assertThat(slug("posts", kept)).isEqualTo("water-tank");
        assertThat(slug("tags", tag)).isEqualTo("tag-" + tag.toString().replace("-", ""));
        assertThat(slug("reports", report)).isEqualTo("report-" + report.toString().replace("-", ""));
    }

    private static UUID post(String slug, long author) {
        UUID id = UUID.randomUUID();
        db.update("INSERT INTO posts (id, title, slug, body, author_pkid) VALUES (?, 'Title', ?, 'Body', ?)",
                id, slug, author);
        return id;
    }

    private static String slug(String table, UUID id) {
        return db.queryForObject("SELECT slug FROM " + table + " WHERE id = ?", String.class, id);
    }
}
