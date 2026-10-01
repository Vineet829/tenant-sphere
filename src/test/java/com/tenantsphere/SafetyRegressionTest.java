package com.tenantsphere;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tenantsphere.common.ContentViewRecorder;
import com.tenantsphere.mail.MailService;
import com.tenantsphere.profile.AvatarService;
import com.tenantsphere.profile.AvatarStorage;
import com.tenantsphere.profile.ProfileRepository;
import com.tenantsphere.report.ReportWriteService;
import com.tenantsphere.report.dto.ReportCreateRequest;
import com.tenantsphere.user.User;
import com.tenantsphere.user.UserRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = "spring.jpa.show-sql=false")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ReportWriteService.class, AvatarService.class, AvatarStorage.class, ContentViewRecorder.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SafetyRegressionTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static final Path AVATARS;
    static {
        try {
            AVATARS = Files.createTempDirectory("tenantsphere-avatar-test-");
        } catch (java.io.IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.media.avatar-dir", AVATARS::toString);
    }

    @AfterAll
    static void cleanup() throws Exception {
        POSTGRES.stop();
        try (var files = Files.list(AVATARS)) {
            for (Path file : files.toList()) Files.deleteIfExists(file);
        }
        Files.deleteIfExists(AVATARS);
    }

    @Autowired private JdbcTemplate database;
    @Autowired private UserRepository users;
    @Autowired private ProfileRepository profiles;
    @Autowired private ReportWriteService reports;
    @Autowired private AvatarService avatars;
    @Autowired private AvatarStorage storage;
    @Autowired private ContentViewRecorder views;
    @Autowired private PlatformTransactionManager transactions;
    @MockBean private MailService mail;

    @BeforeEach
    void clearFixtures() throws Exception {
        // This connection belongs exclusively to the Testcontainers database above.
        database.execute("TRUNCATE users CASCADE");
        database.execute("TRUNCATE content_views");
        try (var files = Files.list(AVATARS)) {
            for (Path file : files.toList()) Files.deleteIfExists(file);
        }
        reset(mail);
    }

    @Test
    void repeatedIncidentsFromOneResidentOnlyCountOnce() {
        User target = user();
        User reporter = user();
        for (int i = 0; i < 10; i++) report(target, reporter);
        assertModeration(target, 1, true);
        assertThat(database.queryForObject("SELECT count(*) FROM reports", Integer.class)).isEqualTo(10);
        verify(mail, times(1)).send(eq(target.getEmail()), startsWith("Warning:"), anyString());
        verify(mail, times(0)).send(eq(target.getEmail()), startsWith("Account Deactivation"), anyString());
    }

    @Test
    void exactlyFiveDifferentResidentsDeactivateAndNotifyOnlyOnce() {
        User target = user();
        for (int count = 1; count <= 6; count++) {
            report(target, user());
            assertModeration(target, count, count < 5);
        }
        verify(mail, times(1)).send(eq(target.getEmail()), startsWith("Warning:"), anyString());
        verify(mail, times(1)).send(eq(target.getEmail()), startsWith("Account Deactivation"), anyString());
    }

    @Test
    void selfReportsAreRejectedWithoutAffectingReputation() {
        User target = user();
        assertThatThrownBy(() -> report(target, target))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertModeration(target, 0, true);
        assertThat(database.queryForObject("SELECT count(*) FROM reports", Integer.class)).isZero();
        verifyNoInteractions(mail);
    }

    @Test
    void concurrentRepeatReportsCannotInflateTheCount() throws Exception {
        User target = user();
        User reporter = user();
        together(10, i -> report(target, reporter));
        assertModeration(target, 1, true);
        verify(mail, times(1)).send(eq(target.getEmail()), startsWith("Warning:"), anyString());
    }

    @Test
    void concurrentDifferentReportersDoNotLoseCountsOrDuplicateDeactivationMail() throws Exception {
        User target = user();
        List<User> reporters = new ArrayList<>();
        for (int i = 0; i < 6; i++) reporters.add(user());
        together(6, i -> report(target, reporters.get(i)));
        assertModeration(target, 6, false);
        verify(mail, times(1)).send(eq(target.getEmail()), startsWith("Warning:"), anyString());
        verify(mail, times(1)).send(eq(target.getEmail()), startsWith("Account Deactivation"), anyString());
    }

    @Test
    void rolledBackReportDoesNotSendMailOrChangeTheCount() {
        User target = user();
        User reporter = user();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            report(target, reporter);
            status.setRollbackOnly();
        });
        assertModeration(target, 0, true);
        verify(mail, times(0)).send(anyString(), anyString(), anyString());
    }

    @Test
    void profileEditsCannotOverwriteANewerAvatarOrReactivateABannedAccount() {
        User target = user();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            var staleProfile = profiles.findByUserPkid(target.getPkid()).orElseThrow();
            var staleUser = users.findById(target.getPkid()).orElseThrow();
            var concurrent = new TransactionTemplate(transactions);
            concurrent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            concurrent.executeWithoutResult(other -> {
                database.update("UPDATE profiles SET avatar = '/media/avatars/new.png', report_count = 5, reputation = 0 WHERE user_pkid = ?", target.getPkid());
                database.update("UPDATE users SET is_active = false WHERE pkid = ?", target.getPkid());
            });
            staleProfile.setBio("Updated bio");
            staleUser.setFirstName("Updated name");
        });
        assertModeration(target, 5, false);
        var profile = profiles.findByUserPkid(target.getPkid()).orElseThrow();
        assertThat(profile.getAvatar()).isEqualTo("/media/avatars/new.png");
        assertThat(profile.getBio()).isEqualTo("Updated bio");
        assertThat(users.findById(target.getPkid()).orElseThrow().getFirstName()).isEqualTo("Updated name");
    }

    @Test
    void genuineSupportedImagesAreSavedAndReplacementsRemoveOnlyTheOldUpload() throws Exception {
        User user = user();
        for (String format : List.of("jpeg", "png", "gif", "webp")) {
            byte[] content = image(format);
            String old = avatar(user);
            avatars.upload(user, upload(format, content));
            String current = avatar(user);
            assertThat(Files.readAllBytes(path(current))).isEqualTo(content);
            if (old != null) assertThat(path(old)).doesNotExist();
            try (var files = Files.list(AVATARS)) {
                assertThat(files.count()).isEqualTo(1);
            }
        }
    }

    @Test
    void forgedCorruptMismatchedAndOversizedImagesAreRejected() throws Exception {
        User user = user();
        List<MockMultipartFile> invalid = List.of(
                upload("png", "not an image".getBytes()),
                upload("jpeg", image("png")),
                upload("png", new byte[0]),
                new MockMultipartFile("avatar", "test.svg", "image/svg+xml", "<svg/>".getBytes()));
        for (var file : invalid) {
            assertThatThrownBy(() -> avatars.upload(user, file))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
        assertThatThrownBy(() -> avatars.upload(user, upload("png", new byte[1024 * 1024 + 1])))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
        assertThat(avatar(user)).isNull();
        try (var files = Files.list(AVATARS)) {
            assertThat(files.count()).isZero();
        }
    }

    @Test
    void excessivePixelDimensionsAreRejectedBeforeDecoding() throws Exception {
        byte[] png = image("png");
        java.nio.ByteBuffer.wrap(png, 16, 8).putInt(5000).putInt(5000);
        var crc = new java.util.zip.CRC32();
        crc.update(png, 12, 17);
        java.nio.ByteBuffer.wrap(png, 29, 4).putInt((int) crc.getValue());
        User user = user();
        assertThatThrownBy(() -> avatars.upload(user, upload("png", png)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getReason()).contains("16 million pixels"));
        assertThat(avatar(user)).isNull();
    }

    @Test
    void avatarRollbackKeepsThePreviousPhotoAndRemovesTheUncommittedFile() throws Exception {
        User user = user();
        avatars.upload(user, upload("png", image("png")));
        String original = avatar(user);
        String reference = UUID.randomUUID() + ".png";
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            storage.store(profilePkid(user), reference, new byte[] {1, 2, 3});
            assertThat(AVATARS.resolve(reference)).exists();
            status.setRollbackOnly();
        });
        assertThat(avatar(user)).isEqualTo(original);
        assertThat(path(original)).exists();
        assertThat(AVATARS.resolve(reference)).doesNotExist();
    }

    @Test
    void storageFailuresAreReportedWithoutDeletingAnExistingFile() throws Exception {
        User user = user();
        avatars.upload(user, upload("png", image("png")));
        String original = avatar(user);
        byte[] bytes = Files.readAllBytes(path(original));
        assertThatThrownBy(() -> storage.store(profilePkid(user), path(original).getFileName().toString(), new byte[] {1}))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(avatar(user)).isEqualTo(original);
        assertThat(Files.readAllBytes(path(original))).isEqualTo(bytes);
    }

    @Test
    void deletionAndCascadedDeletionRemoveOnlyTheirOwnViews() {
        User author = user();
        long post = post(author);
        long remaining = post(author);
        views.record("post", post, author, new MockHttpServletRequest());
        views.record("post", remaining, author, new MockHttpServletRequest());
        database.update("DELETE FROM posts WHERE pkid = ?", post);
        assertViews("post", post, 0);
        assertViews("post", remaining, 1);

        long apartment = database.queryForObject("""
                INSERT INTO apartments (id, unit_number, building, floor, tenant_pkid)
                VALUES (?, 'QA-101', 'QA', 1, ?) RETURNING pkid
                """, Long.class, UUID.randomUUID(), author.getPkid());
        long issue = database.queryForObject("""
                INSERT INTO issues (id, apartment_pkid, reported_by_pkid, title, description)
                VALUES (?, ?, ?, 'QA issue', 'Cleanup test') RETURNING pkid
                """, Long.class, UUID.randomUUID(), apartment, author.getPkid());
        views.record("issue", issue, author, new MockHttpServletRequest());
        database.update("DELETE FROM apartments WHERE pkid = ?", apartment);
        assertViews("issue", issue, 0);
        assertViews("post", remaining, 1);
    }

    @Test
    void failedDeletionRollsBackTheViewCleanupAsWell() {
        User author = user();
        long post = post(author);
        views.record("post", post, author, new MockHttpServletRequest());
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            database.update("DELETE FROM posts WHERE pkid = ?", post);
            assertViews("post", post, 0);
            status.setRollbackOnly();
        });
        assertViews("post", post, 1);
    }

    @Test
    void concurrentOrStalePageViewsCannotRecreateDeletedContentViews() throws Exception {
        User author = user();
        long post = post(author);
        together(2, i -> {
            if (i == 0) {
                for (int j = 0; j < 10; j++) views.record("post", post, author, new MockHttpServletRequest());
            } else {
                database.update("DELETE FROM posts WHERE pkid = ?", post);
            }
        });
        views.record("post", post, author, new MockHttpServletRequest());
        assertViews("post", post, 0);
    }

    private User user() {
        String name = "qa_" + UUID.randomUUID().toString().replace("-", "");
        long pkid = database.queryForObject("""
                INSERT INTO users (id, email, username, first_name, last_name, password, is_active)
                VALUES (?, ?, ?, 'QA', 'Resident', '!unused', true) RETURNING pkid
                """, Long.class, UUID.randomUUID(), name + "@example.invalid", name);
        database.update("INSERT INTO profiles (id, user_pkid, slug) VALUES (?, ?, ?)", UUID.randomUUID(), pkid, name);
        return users.findById(pkid).orElseThrow();
    }

    private void report(User target, User reporter) {
        reports.create(new ReportCreateRequest("QA " + UUID.randomUUID(), "QA incident", target.getUsername()), reporter);
    }

    private void assertModeration(User target, int count, boolean active) {
        var profile = profiles.findByUserPkid(target.getPkid()).orElseThrow();
        assertThat(profile.getReportCount()).isEqualTo(count);
        assertThat(profile.getReputation()).isEqualTo(Math.max(0, 100 - count * 20));
        assertThat(users.findById(target.getPkid()).orElseThrow().isActive()).isEqualTo(active);
    }

    private String avatar(User user) {
        return profiles.findByUserPkid(user.getPkid()).orElseThrow().getAvatar();
    }

    private Long profilePkid(User user) {
        return profiles.findByUserPkid(user.getPkid()).orElseThrow().getPkid();
    }

    private Path path(String avatar) {
        return AVATARS.resolve(avatar.substring("/media/avatars/".length()));
    }

    private byte[] image(String format) throws Exception {
        if (format.equals("webp")) {
            try (var input = getClass().getResourceAsStream("/avatar.webp")) {
                return input.readAllBytes();
            }
        }
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), format, bytes);
        return bytes.toByteArray();
    }

    private MockMultipartFile upload(String format, byte[] bytes) {
        return new MockMultipartFile("avatar", "photo." + format, "image/" + format, bytes);
    }

    private long post(User author) {
        UUID id = UUID.randomUUID();
        return database.queryForObject("""
                INSERT INTO posts (id, title, slug, body, author_pkid)
                VALUES (?, 'QA post', ?, 'Cleanup test', ?) RETURNING pkid
                """, Long.class, id, "qa-" + id, author.getPkid());
    }

    private void assertViews(String type, long pkid, int expected) {
        assertThat(database.queryForObject("SELECT count(*) FROM content_views WHERE content_type = ? AND object_pkid = ?",
                Integer.class, type, pkid)).isEqualTo(expected);
    }

    private void together(int count, java.util.function.IntConsumer action) throws Exception {
        var ready = new CountDownLatch(count);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(count)) {
            List<Future<?>> tasks = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int index = i;
                tasks.add(executor.submit(() -> {
                    ready.countDown();
                    try {
                        if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Start timed out");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                    action.accept(index);
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> task : tasks) task.get(20, TimeUnit.SECONDS);
        }
    }
}
