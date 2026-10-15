package com.tenantsphere.post;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenantsphere.common.ContentViewRecorder;
import com.tenantsphere.profile.Profile;
import com.tenantsphere.profile.ProfileRepository;
import com.tenantsphere.user.User;
import com.tenantsphere.user.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.testcontainers.containers.PostgreSQLContainer;

// Pages of posts are read the way a request reads them: no transaction around the call, so tags
// that load after the page's own transaction has ended would fail.
@DataJpaTest(properties = "spring.jpa.show-sql=false")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostController.class, PostService.class, ContentViewRecorder.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PostListTransactionTest {
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

    @Autowired private PostController posts;
    @Autowired private PostRepository postRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private UserRepository users;
    @Autowired private ProfileRepository profiles;
    @Autowired private PlatformTransactionManager transactions;

    @BeforeEach
    void request() {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest("GET", "/api/v1/posts/")));
    }

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void aPageOfTaggedPostsListsTheirTags() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            User author = new User();
            author.setUsername("page_author");
            author.setEmail("page_author@example.invalid");
            author.setFirstName("Page");
            author.setLastName("Author");
            author.setPassword("not-used");
            author.setActive(true);
            users.save(author);
            Profile profile = new Profile();
            profile.setUser(author);
            profile.setSlug("page-author");
            profiles.save(profile);
            Tag tag = new Tag();
            tag.setName("Plumbing");
            tag.setSlug("plumbing");
            tagRepository.save(tag);
            Post post = new Post();
            post.setTitle("A leaking tap");
            post.setSlug("a-leaking-tap");
            post.setBody("The kitchen tap in 4B drips all night.");
            post.setAuthor(author);
            post.getTags().add(tag);
            postRepository.save(post);
        });

        var page = posts.list(null, 1);

        assertThat(page.results()).singleElement().satisfies(post -> assertThat(post.tags()).containsExactly("Plumbing"));
        assertThat(posts.byTag("plumbing", null, 1).results()).hasSize(1);
    }
}
