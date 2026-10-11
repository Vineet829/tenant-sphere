package com.tenantsphere.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.tenantsphere.post.dto.ReplyResponse;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostReplyService.class)
class PostReplyServiceTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @AfterAll
    static void stopDatabase() {
        POSTGRES.stop();
    }

    @Autowired private PostReplyService replies;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private JdbcTemplate database;

    @Test
    void aPageOfRepliesLoadsItsAuthorsAvatarsAtOnce() {
        long first = person("first_replier", "/media/avatars/first.png");
        long second = person("second_replier", null);
        long third = person("third_replier", "/media/avatars/third.png");
        UUID post = UUID.randomUUID();
        long postPkid = database.queryForObject("""
                INSERT INTO posts (id, title, slug, body, author_pkid) VALUES (?, 'A post', 'a-post', 'Body', ?)
                RETURNING pkid
                """, Long.class, post, first);
        for (long author : new long[] {first, second, third, first}) {
            database.update("INSERT INTO replies (id, post_pkid, author_pkid, body) VALUES (?, ?, ?, 'Reply')",
                    UUID.randomUUID(), postPkid, author);
        }
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        var page = replies.listNewestFirst(post, PageRequest.of(0, 10)).getContent();

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
        assertThat(page).extracting(ReplyResponse::authorUsername, ReplyResponse::avatar).containsExactlyInAnyOrder(
                tuple("first_replier", "/media/avatars/first.png"), tuple("second_replier", null),
                tuple("third_replier", "/media/avatars/third.png"), tuple("first_replier", "/media/avatars/first.png"));
    }

    private long person(String username, String avatar) {
        long user = database.queryForObject("""
                INSERT INTO users (id, email, username, first_name, last_name, password, is_active)
                VALUES (?, ?, ?, 'Test', 'Person', '!unused', true) RETURNING pkid
                """, Long.class, UUID.randomUUID(), username + "@example.test", username);
        database.update("INSERT INTO profiles (id, user_pkid, slug, avatar) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), user, username, avatar);
        return user;
    }
}
