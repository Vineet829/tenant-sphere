package com.tenantsphere.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tenantsphere.auth.AppUserDetails;
import com.tenantsphere.user.User;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = "spring.jpa.show-sql=false")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostVoteService.class, PostVoteRepository.class, PostService.class})
class PostVoteServiceTest {

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

    @Autowired private PostVoteService votes;
    @Autowired private PostService responses;
    @Autowired private PostRepository posts;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate database;

    @Test
    void upvoteCanBeAddedRemovedAndAddedAgainWithAccurateMessages() {
        User voter = user("voter");
        UUID post = post(voter);
        var controller = new PostVoteController(votes);
        var principal = new AppUserDetails(voter);

        assertThat(controller.upvote(post, principal).message()).isEqualTo("Post upvoted successfully!");
        assertVotes(post, voter, 1, 0, true);

        var removed = controller.upvote(post, principal);
        assertVotes(post, voter, 0, 0, false);
        assertThat(removed.message()).isEqualTo("Upvote removed");

        assertThat(controller.upvote(post, principal).message()).isEqualTo("Post upvoted successfully!");
        assertVotes(post, voter, 1, 0, true);
    }

    @Test
    void removingAnUpvotePreservesOtherPeoplesVotes() {
        User voter = user("voter");
        User neighbour = user("neighbour");
        User other = user("other");
        UUID post = post(voter);
        votes.upvote(post, neighbour);
        votes.downvote(post, other);
        votes.upvote(post, voter);
        assertVotes(post, voter, 2, 1, true);

        votes.upvote(post, voter);

        assertVotes(post, voter, 1, 1, false);
        Post stored = posts.findByExternalId(post).orElseThrow();
        assertThat(stored.getUpvotedBy()).extracting(User::getPkid)
                .containsExactly(neighbour.getPkid());
        assertThat(stored.getDownvotedBy()).extracting(User::getPkid)
                .containsExactly(other.getPkid());
    }

    @Test
    void switchingVotesReplacesThePreviousVoteAndEitherVoteCanBeRemoved() {
        User voter = user("voter");
        UUID post = post(voter);
        votes.upvote(post, voter);
        assertVotes(post, voter, 1, 0, true);

        votes.downvote(post, voter);
        assertVotes(post, voter, 0, 1, false);
        votes.upvote(post, voter);
        assertVotes(post, voter, 1, 0, true);
        votes.upvote(post, voter);
        assertVotes(post, voter, 0, 0, false);

        votes.downvote(post, voter);
        assertVotes(post, voter, 0, 1, false);
        votes.downvote(post, voter);
        assertVotes(post, voter, 0, 0, false);
    }

    @Test
    void downvotingAtZeroProducesANegativeScoreAndUndoRestoresZero() {
        User voter = user("voter");
        UUID post = post(voter);
        var controller = new PostVoteController(votes);
        var principal = new AppUserDetails(voter);

        assertThat(controller.downvote(post, principal).message()).isEqualTo("Post downvoted successfully!");
        assertVotes(post, voter, 0, 1, false);
        Post stored = posts.findByExternalId(post).orElseThrow();
        assertThat(stored.getUpvotes() - stored.getDownvotes()).isEqualTo(-1);
        assertVoteResponses(post, voter, false, true);

        assertThat(controller.downvote(post, principal).message()).isEqualTo("Downvote removed");
        assertVotes(post, voter, 0, 0, false);
        assertVoteResponses(post, voter, false, false);
    }

    @Test
    void selectedVoteIsScopedToTheViewerAndUpdatesWhenSwitchingDirections() {
        User voter = user("voter");
        User neighbour = user("neighbour");
        UUID post = post(voter);
        votes.downvote(post, voter);
        votes.upvote(post, neighbour);
        assertVotes(post, voter, 1, 1, false);
        assertVoteResponses(post, voter, false, true);
        assertVoteResponses(post, neighbour, true, false);
        assertVoteResponses(post, null, false, false);

        votes.upvote(post, voter);
        assertVotes(post, voter, 2, 0, true);
        assertVoteResponses(post, voter, true, false);
        votes.downvote(post, voter);
        assertVotes(post, voter, 1, 1, false);
        assertVoteResponses(post, voter, false, true);
    }

    @Test
    void votingOnAMissingPostReturnsNotFound() {
        User voter = user("voter");
        assertThatThrownBy(() -> votes.upvote(UUID.randomUUID(), voter))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void everyExplicitVoteTransitionIsIdempotentAndDoesNotMarkThePostAsEdited() {
        User voter = user("voter");
        UUID post = post(voter);
        var updated = database.queryForObject("SELECT updated_at FROM posts WHERE id = ?", java.sql.Timestamp.class, post);
        for (int from : new int[] {-1, 0, 1}) {
            for (int to : new int[] {-1, 0, 1}) {
                votes.setVote(post, voter, from);
                for (int attempt = 0; attempt < 2; attempt++) {
                    var result = votes.setVote(post, voter, to);
                    assertThat(result.upvotes()).isEqualTo(to == 1 ? 1 : 0);
                    assertThat(result.downvotes()).isEqualTo(to == -1 ? 1 : 0);
                    assertThat(result.isUpvoted()).isEqualTo(to == 1);
                    assertThat(result.isDownvoted()).isEqualTo(to == -1);
                    assertVotes(post, voter, to == 1 ? 1 : 0, to == -1 ? 1 : 0, to == 1);
                }
            }
        }
        assertThat(database.queryForObject("SELECT updated_at FROM posts WHERE id = ?", java.sql.Timestamp.class, post)).isEqualTo(updated);
    }

    @Test
    void editingAStalePostCannotOverwriteVotesAndBadCountersAreReconciled() {
        User voter = user("voter");
        UUID post = post(voter);
        Post stale = posts.findByExternalId(post).orElseThrow();
        database.update("UPDATE posts SET upvotes = 99, downvotes = 17 WHERE id = ?", post);
        var result = votes.setVote(post, voter, 1);
        assertThat(result.upvotes()).isEqualTo(1);
        assertThat(result.downvotes()).isZero();
        stale.setBody("Edited while a vote was saved");
        posts.saveAndFlush(stale);
        assertVotes(post, voter, 1, 0, true);
        assertThat(posts.findByExternalId(post).orElseThrow().getBody()).isEqualTo("Edited while a vote was saved");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentVotesFromDifferentPeopleDoNotLoseCounts() throws Exception {
        List<User> voters = new ArrayList<>();
        UUID post = null;
        try {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            for (int index = 0; index < 12; index++) voters.add(user("parallel-" + suffix + "-" + index));
            post = post(voters.getFirst());
            UUID target = post;
            runTogether(12, index -> votes.setVote(target, voters.get(index), index % 2 == 0 ? 1 : -1));
            assertStoredCounts(post, 6, 6);
        } finally {
            if (post != null) database.update("DELETE FROM posts WHERE id = ?", post);
            for (User voter : voters) database.update("DELETE FROM users WHERE pkid = ?", voter.getPkid());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentSubmissionsFromOnePersonAreIdempotentAndMutuallyExclusive() throws Exception {
        User voter = user("parallel-" + UUID.randomUUID().toString().substring(0, 8));
        UUID post = post(voter);
        try {
            runTogether(10, index -> votes.setVote(post, voter, 1));
            assertStoredCounts(post, 1, 0);
            runTogether(10, index -> votes.setVote(post, voter, index % 2 == 0 ? 1 : -1));
            int upvotes = database.queryForObject("SELECT upvotes FROM posts WHERE id = ?", Integer.class, post);
            assertStoredCounts(post, upvotes, 1 - upvotes);
        } finally {
            database.update("DELETE FROM posts WHERE id = ?", post);
            database.update("DELETE FROM users WHERE pkid = ?", voter.getPkid());
        }
    }

    private void runTogether(int count, java.util.function.IntConsumer action) throws Exception {
        var ready = new CountDownLatch(count);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(count)) {
            List<Future<?>> work = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                int task = index;
                work.add(executor.submit(() -> {
                    ready.countDown();
                    try {
                        if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Vote start timed out");
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(exception);
                    }
                    action.accept(task);
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> task : work) task.get(20, TimeUnit.SECONDS);
        }
    }

    @Test
    void postListsArePagedByTheDatabaseAndKeepEveryTagOfEachPost() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        User author = user("tagger_" + suffix);
        long garden = tag("garden-" + suffix);
        long noise = tag("noise-" + suffix);
        for (int i = 0; i < 3; i++) {
            UUID postId = post(author);
            for (long tag : List.of(garden, noise)) {
                database.update("INSERT INTO post_tags (post_pkid, tag_pkid) VALUES ((SELECT pkid FROM posts WHERE id = ?), ?)",
                        postId, tag);
            }
        }
        entityManager.flush();
        entityManager.clear();

        // With fail_on_pagination_over_collection_fetch on, a page that fetched the tags would be refused.
        var page = posts.findByAuthor(author.getPkid(), PageRequest.of(0, 2));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(responses.toResponses(page, author).getContent())
                .allSatisfy(post -> assertThat(post.tags()).containsExactly("garden-" + suffix, "noise-" + suffix));
        var tagged = posts.findByTagSlug("garden-" + suffix, PageRequest.of(0, 10));
        assertThat(responses.toTagResponses(tagged, author).getContent())
                .hasSize(3)
                .allSatisfy(post -> assertThat(post.tags()).containsExactly("garden-" + suffix, "noise-" + suffix));
    }

    private long tag(String name) {
        return database.queryForObject("INSERT INTO tags (name, slug) VALUES (?, ?) RETURNING pkid", Long.class, name, name);
    }

    @Test
    void aListCountsEachPostsRepliesAndOnlyThePostsOwnPageShowsThem() {
        User author = user("lister_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        UUID postId = post(author);
        for (int i = 0; i < 2; i++) {
            database.update("""
                    INSERT INTO replies (id, post_pkid, author_pkid, body)
                    VALUES (?, (SELECT pkid FROM posts WHERE id = ?), ?, 'A reply')
                    """, UUID.randomUUID(), postId, author.getPkid());
        }
        Post stored = posts.findByExternalId(postId).orElseThrow();

        var listed = responses.toResponses(new PageImpl<>(List.of(stored)), author).getContent().getFirst();
        assertThat(listed.repliesCount()).isEqualTo(2);
        assertThat(listed.replies()).isEmpty();
        assertThat(responses.toResponse(stored, author).replies()).hasSize(2);
    }

    private void assertStoredCounts(UUID post, int upvotes, int downvotes) {
        assertThat(database.queryForObject("SELECT upvotes FROM posts WHERE id = ?", Integer.class, post)).isEqualTo(upvotes);
        assertThat(database.queryForObject("SELECT downvotes FROM posts WHERE id = ?", Integer.class, post)).isEqualTo(downvotes);
        assertThat(database.queryForObject("SELECT count(*) FROM post_upvotes v JOIN posts p ON p.pkid = v.post_pkid WHERE p.id = ?", Integer.class, post)).isEqualTo(upvotes);
        assertThat(database.queryForObject("SELECT count(*) FROM post_downvotes v JOIN posts p ON p.pkid = v.post_pkid WHERE p.id = ?", Integer.class, post)).isEqualTo(downvotes);
        assertThat(database.queryForObject("SELECT count(*) FROM post_upvotes u JOIN post_downvotes d USING (post_pkid, user_pkid) JOIN posts p ON p.pkid = u.post_pkid WHERE p.id = ?", Integer.class, post)).isZero();
    }

    private void assertVotes(UUID postId, User voter, int upvotes, int downvotes, boolean upvoted) {
        entityManager.flush();
        entityManager.clear();
        Post stored = posts.findByExternalId(postId).orElseThrow();
        assertThat(stored.getUpvotes()).isEqualTo(upvotes);
        assertThat(stored.getDownvotes()).isEqualTo(downvotes);
        assertThat(stored.getUpvotedBy()).hasSize(upvotes);
        assertThat(stored.getDownvotedBy()).hasSize(downvotes);
        assertThat(posts.findUpvotedPkids(voter.getPkid(), List.of(stored.getPkid())))
                .isEqualTo(upvoted ? List.of(stored.getPkid()) : List.of());
    }

    private void assertVoteResponses(UUID postId, User voter, boolean upvoted, boolean downvoted) {
        Post stored = posts.findByExternalId(postId).orElseThrow();
        var detail = responses.toResponse(stored, voter);
        assertThat(detail.isUpvoted()).isEqualTo(upvoted);
        assertThat(detail.isDownvoted()).isEqualTo(downvoted);
        var page = new PageImpl<>(List.of(stored));
        var feed = responses.toResponses(page, voter).getContent().getFirst();
        assertThat(feed.isUpvoted()).isEqualTo(upvoted);
        assertThat(feed.isDownvoted()).isEqualTo(downvoted);
        var tagged = responses.toTagResponses(page, voter).getContent().getFirst();
        assertThat(tagged.isUpvoted()).isEqualTo(upvoted);
        assertThat(tagged.isDownvoted()).isEqualTo(downvoted);
        var top = responses.toTopResponses(List.of(stored)).getFirst();
        assertThat(top.upvotes()).isEqualTo(detail.upvotes());
        assertThat(top.downvotes()).isEqualTo(detail.downvotes());
    }

    private User user(String username) {
        Long pkid = database.queryForObject("""
                INSERT INTO users (id, email, username, first_name, last_name, password)
                VALUES (?, ?, ?, 'Test', 'Voter', '!unused') RETURNING pkid
                """, Long.class, UUID.randomUUID(), username + "@example.test", username);
        return entityManager.find(User.class, pkid);
    }

    private UUID post(User author) {
        UUID id = UUID.randomUUID();
        database.update("""
                INSERT INTO posts (id, title, slug, body, author_pkid)
                VALUES (?, 'Voting test', ?, 'Test post', ?)
                """, id, "voting-" + id, author.getPkid());
        return id;
    }
}
