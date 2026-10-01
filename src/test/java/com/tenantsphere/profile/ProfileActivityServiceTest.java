package com.tenantsphere.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tenantsphere.profile.dto.ProfilePostResponse;
import com.tenantsphere.profile.dto.ProfileReviewsResponse.RatingBucket;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = "spring.jpa.show-sql=false")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProfileActivityService.class)
class ProfileActivityServiceTest {

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

    @Autowired private ProfileActivityService activity;
    @Autowired private JdbcTemplate database;

    @Test
    void postsAreScopedToTheProfileAndNewestFirstWithReplyCounts() {
        long author = profile("neighbour", "tenant", null);
        long other = profile("other", "tenant", null);
        long older = post("popular-older", author, 20, 48);
        long newest = post("newest", author, 1, 1);
        post("middle", author, 3, 24);
        long unrelated = post("someone-else", other, 100, 0);
        reply(newest, other);
        reply(newest, author);
        reply(older, other);
        reply(unrelated, author);

        var first = activity.posts("neighbour", PageRequest.of(0, 2));
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getContent()).extracting(ProfilePostResponse::slug)
                .containsExactly("newest", "middle");
        assertThat(first.getContent()).extracting(ProfilePostResponse::repliesCount)
                .containsExactly(2L, 0L);
        assertThat(first.getContent().getFirst().body()).isEqualTo("Post body for newest");
        var second = activity.posts("neighbour", PageRequest.of(1, 2));
        assertThat(second.getContent()).extracting(ProfilePostResponse::slug)
                .containsExactly("popular-older");
        assertThat(second.hasNext()).isFalse();
    }

    @Test
    void postsWithTheSameTimestampHaveStablePageOrder() {
        long author = profile("neighbour", "tenant", null);
        post("first", author, 0, 1);
        post("second", author, 0, 1);
        assertThat(activity.posts("neighbour", PageRequest.of(0, 1)).getContent())
                .extracting(ProfilePostResponse::slug).containsExactly("second");
        assertThat(activity.posts("neighbour", PageRequest.of(1, 1)).getContent())
                .extracting(ProfilePostResponse::slug).containsExactly("first");
    }

    @Test
    void reviewsIncludeAuthorsAndTheSummaryCoversAllPagesForOnlyThisPerson() {
        long technician = profile("electrician", "electrician", null);
        long otherTechnician = profile("plumber", "plumber", null);
        long first = profile("first-reviewer", "tenant", "/media/first.jpg");
        long second = profile("second-reviewer", "tenant", null);
        long third = profile("third-reviewer", "tenant", null);
        rating(technician, first, 5, "Explained the repair clearly.", 1);
        rating(technician, second, 4, "Arrived on time.", 24);
        rating(technician, third, 2, "", 48);
        rating(otherTechnician, first, 1, "Unrelated review.", 0);

        var result = activity.reviews("electrician", PageRequest.of(0, 1), "/reviews/?page_size=1");
        assertThat(result.averageRating()).isEqualTo(11.0 / 3);
        assertThat(result.distribution()).containsExactly(
                new RatingBucket(5, 1), new RatingBucket(4, 1), new RatingBucket(3, 0),
                new RatingBucket(2, 1), new RatingBucket(1, 0));
        assertThat(result.reviews().count()).isEqualTo(3);
        assertThat(result.reviews().next()).isEqualTo("/reviews/?page_size=1&page=2");
        assertThat(result.reviews().previous()).isNull();
        var review = result.reviews().results().getFirst();
        assertThat(review.authorUsername()).isEqualTo("first-reviewer");
        assertThat(review.authorName()).isNotBlank();
        assertThat(review.avatar()).isEqualTo("/media/first.jpg");
        assertThat(review.comment()).isEqualTo("Explained the repair clearly.");
        assertThat(review.createdAt()).isNotNull();

        var last = activity.reviews("electrician", PageRequest.of(2, 1), "/reviews/?page_size=1");
        assertThat(last.averageRating()).isEqualTo(result.averageRating());
        assertThat(last.reviews().next()).isNull();
        assertThat(last.reviews().results().getFirst().comment()).isEmpty();
        assertThat(last.reviews().results().getFirst().avatar()).isNull();
    }

    @Test
    void profilesWithoutActivityHaveEmptyResultsAndZeroRatingBuckets() {
        profile("new-neighbour", "tenant", null);
        assertThat(activity.posts("new-neighbour", PageRequest.of(0, 3))).isEmpty();
        var result = activity.reviews("new-neighbour", PageRequest.of(0, 3), "/reviews/");
        assertThat(result.averageRating()).isZero();
        assertThat(result.reviews().count()).isZero();
        assertThat(result.reviews().results()).isEmpty();
        assertThat(result.distribution()).hasSize(5)
                .allSatisfy(bucket -> assertThat(bucket.count()).isZero());
    }

    @Test
    void missingAndNonDirectoryProfilesAreNotExposed() {
        long staff = profile("staff", "tenant", null);
        long admin = profile("admin", "tenant", null);
        database.update("UPDATE users SET is_staff = TRUE WHERE pkid = ?", staff);
        database.update("UPDATE users SET is_superuser = TRUE WHERE pkid = ?", admin);
        for (String slug : new String[] {"missing", "staff", "admin"}) {
            assertThatThrownBy(() -> activity.posts(slug, PageRequest.of(0, 3)))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
            assertThatThrownBy(() -> activity.reviews(slug, PageRequest.of(0, 3), "/reviews/"))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    private long profile(String username, String occupation, String avatar) {
        long user = database.queryForObject("""
                INSERT INTO users (id, email, username, first_name, last_name, password)
                VALUES (?, ?, ?, 'Test', 'Neighbour', '!unused') RETURNING pkid
                """, Long.class, UUID.randomUUID(), username + "@example.test", username);
        database.update("""
                INSERT INTO profiles (id, user_pkid, slug, occupation, avatar)
                VALUES (?, ?, ?, ?, ?)
                """, UUID.randomUUID(), user, username, occupation, avatar);
        return user;
    }

    private long post(String slug, long author, int votes, int hoursAgo) {
        return database.queryForObject("""
                INSERT INTO posts (id, title, slug, body, author_pkid, upvotes, created_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW() - ? * INTERVAL '1 hour') RETURNING pkid
                """, Long.class, UUID.randomUUID(), slug, slug, "Post body for " + slug,
                author, votes, hoursAgo);
    }

    private void reply(long post, long author) {
        database.update("""
                INSERT INTO replies (id, post_pkid, author_pkid, body)
                VALUES (?, ?, ?, 'A reply')
                """, UUID.randomUUID(), post, author);
    }

    private void rating(long ratedUser, long author, int stars, String comment, int hoursAgo) {
        database.update("""
                INSERT INTO ratings (id, rated_user_pkid, rating_user_pkid, rating, comment, created_at)
                VALUES (?, ?, ?, ?, ?, NOW() - ? * INTERVAL '1 hour')
                """, UUID.randomUUID(), ratedUser, author, stars, comment, hoursAgo);
    }
}
