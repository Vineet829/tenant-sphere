package com.tenantsphere.post;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostRepositoryTest {

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

    @Autowired private PostRepository posts;
    @Autowired private JdbcTemplate database;

    @Test
    void topPostsFetchAuthorsAndRankByVotesThenViewsThenRecency() {
        long firstAuthor = author("first");
        long secondAuthor = author("second");
        long mostVotes = post("most-votes", firstAuthor, 3, 72);
        long mostViews = post("most-views", secondAuthor, 2, 48);
        long newest = post("newest", secondAuthor, 2, 1);
        long oldest = post("oldest", firstAuthor, 2, 96);
        views(mostVotes, 1);
        views(mostViews, 3);
        views(newest, 1);
        views(oldest, 1);

        var result = posts.findTopPosts(PageRequest.of(0, 3));

        assertThat(result).extracting(Post::getSlug)
                .containsExactly("most-votes", "most-views", "newest");
        assertThat(result).extracting(post -> post.getAuthor().getUsername())
                .containsExactly("first", "second", "second");
    }

    @Test
    void storedViewCountsFollowNewRepeatedMovedAndDeletedViews() {
        long author = author("view_author");
        long viewer = author("repeat_viewer");
        long first = post("first-viewed", author, 0, 2);
        long second = post("second-viewed", author, 0, 1);
        views(first, 2);
        String recordView = """
                INSERT INTO content_views (id, content_type, object_pkid, user_pkid, viewer_ip)
                VALUES (?, 'post', ?, ?, '198.51.100.7')
                ON CONFLICT ON CONSTRAINT uq_content_views
                DO UPDATE SET last_viewed = NOW(), updated_at = NOW()
                """;
        database.update(recordView, UUID.randomUUID(), first, viewer);
        database.update(recordView, UUID.randomUUID(), first, viewer);
        assertThat(viewCount(first)).isEqualTo(3);

        database.update("UPDATE content_views SET object_pkid = ? WHERE user_pkid = ?", second, viewer);
        assertThat(viewCount(first)).isEqualTo(2);
        assertThat(viewCount(second)).isEqualTo(1);

        database.update("DELETE FROM users WHERE pkid = ?", viewer);
        assertThat(viewCount(second)).isEqualTo(1);
        database.update("DELETE FROM content_views WHERE object_pkid = ?", first);
        assertThat(viewCount(first)).isZero();
    }

    private int viewCount(long post) {
        return database.queryForObject("SELECT view_count FROM posts WHERE pkid = ?", Integer.class, post);
    }

    private long author(String username) {
        return database.queryForObject("""
                INSERT INTO users (id, email, username, first_name, last_name, password)
                VALUES (?, ?, ?, 'Test', 'Author', '!unused') RETURNING pkid
                """, Long.class, UUID.randomUUID(), username + "@example.test", username);
    }

    @Test
    void topPostsUseNetScoreAndTheCommunityFeedKeepsItsNewestFirstOrder() {
        long author = author("author");
        long negative = post("negative", author, 12, 1);
        database.update("UPDATE posts SET downvotes = 15 WHERE pkid = ?", negative);
        post("positive", author, 2, 48);
        post("neutral", author, 0, 24);

        assertThat(posts.findTopPosts(PageRequest.of(0, 3))).extracting(Post::getSlug)
                .containsExactly("positive", "neutral", "negative");
        assertThat(posts.findAllOrdered(PageRequest.of(0, 3))).extracting(Post::getSlug)
                .containsExactly("negative", "neutral", "positive");
        assertThat(posts.findByAuthor(author, PageRequest.of(0, 3))).extracting(Post::getSlug)
                .containsExactly("negative", "neutral", "positive");
    }

    private long post(String slug, long author, int votes, int hoursAgo) {
        return database.queryForObject("""
                INSERT INTO posts (id, title, slug, body, author_pkid, upvotes, created_at)
                VALUES (?, ?, ?, 'Test post', ?, ?, NOW() - ? * INTERVAL '1 hour')
                RETURNING pkid
                """, Long.class, UUID.randomUUID(), slug, slug, author, votes, hoursAgo);
    }

    private void views(long post, int count) {
        for (int index = 0; index < count; index++) {
            database.update("""
                    INSERT INTO content_views (id, content_type, object_pkid, viewer_ip)
                    VALUES (?, 'post', ?, ?)
                    """, UUID.randomUUID(), post, "192.0.2." + (index + 1));
        }
    }
}
