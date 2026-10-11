package com.tenantsphere;

import com.tenantsphere.auth.*;
import com.tenantsphere.issue.*;
import com.tenantsphere.issue.dto.IssueStatusUpdateRequest;
import com.tenantsphere.common.ContentViewRecorder;
import com.tenantsphere.profile.AvatarStorage;
import com.tenantsphere.mail.MailService;
import com.tenantsphere.report.ReportWriteService;
import com.tenantsphere.report.dto.ReportCreateRequest;
import com.tenantsphere.user.User;
import com.tenantsphere.user.UserRepository;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@DataJpaTest(properties = {"spring.jpa.show-sql=false", "spring.devtools.livereload.enabled=false", "app.guest.enabled=true"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GuestService.class, GuestCleanup.class, RequestLimiter.class, PendingSignUps.class, AvatarStorage.class, IssueWriteService.class,
        ContentViewRecorder.class, ReportWriteService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BugReportDatabaseTest {
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        POSTGRES.start();
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @AfterAll static void stop() { POSTGRES.stop(); }
    @Autowired JdbcTemplate db;
    @Autowired GuestService guests;
    @Autowired GuestCleanup cleanup;
    @Autowired UserRepository users;
    @Autowired IssueWriteService issues;
    @Autowired ContentViewRecorder views;
    @Autowired ReportWriteService reports;
    @MockBean MailService mail;
    @MockBean TokenRevocations revocations;
    @BeforeEach void resetDatabase() { db.execute("TRUNCATE users,guest_login_limits CASCADE"); reset(mail); }

    @Test void guestsGetSeparatePrivateDataAndCleanupPreservesResidentsAndVoteCounts() {
        User resident = resident();
        long post = post(resident);
        User first = guests.create("198.51.100.1"); User second = guests.create("198.51.100.1");
        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(first.isStaff() || first.isSuperuser()).isFalse();
        assertThat(first.getPassword()).startsWith("!");
        assertThat(count("SELECT count(*) FROM issues WHERE reported_by_pkid=?", first.getPkid())).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM apartments WHERE tenant_pkid=?", second.getPkid())).isEqualTo(1);
        db.update("INSERT INTO post_upvotes(post_pkid,user_pkid) VALUES (?,?),(?,?)", post, first.getPkid(), post, resident.getPkid());
        db.update("UPDATE posts SET upvotes=2 WHERE pkid=?", post);
        db.update("UPDATE users SET guest_expires_at=NOW()-INTERVAL '1 minute' WHERE pkid=?", first.getPkid());
        cleanup.removeExpired();
        assertThat(users.findById(first.getPkid())).isEmpty();
        assertThat(users.findById(second.getPkid())).isPresent();
        assertThat(users.findById(resident.getPkid())).isPresent();
        assertThat(count("SELECT count(*) FROM apartments WHERE tenant_pkid=?", first.getPkid())).isZero();
        assertThat(count("SELECT upvotes FROM posts WHERE pkid=?", post)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM issues WHERE reported_by_pkid=?", second.getPkid())).isEqualTo(2);
    }

    @Test void demoReportsNeverCountTowardFiveDistinctResidents() {
        User target = resident();
        for (int i = 0; i < 5; i++) {
            User guest = guests.create("198.51.100." + i);
            reports.create(new ReportCreateRequest("Guest report " + i, "Demo", target.getUsername()), guest);
        }
        assertThat(count("SELECT report_count FROM profiles WHERE user_pkid=?", target.getPkid())).isZero();
        verifyNoInteractions(mail);
        for (int i = 0; i < 5; i++) reports.create(new ReportCreateRequest("Resident report " + i, "Incident", target.getUsername()), resident());
        assertThat(count("SELECT report_count FROM profiles WHERE user_pkid=?", target.getPkid())).isEqualTo(5);
        assertThat(users.findById(target.getPkid()).orElseThrow().isActive()).isFalse();
    }

    @Test void reportsFromFiveResidentsKeepAStaffAccountOn() {
        User staff = resident();
        db.update("UPDATE users SET is_staff=TRUE WHERE pkid=?", staff.getPkid());
        for (int i = 0; i < 5; i++) reports.create(new ReportCreateRequest("Report " + i, "Incident", staff.getUsername()), resident());
        assertThat(count("SELECT report_count FROM profiles WHERE user_pkid=?", staff.getPkid())).isEqualTo(5);
        assertThat(users.findById(staff.getPkid()).orElseThrow().isActive()).isTrue();
    }

    @Test void concurrentResolutionSendsOnlyOneNotification() throws Exception {
        User guest = guests.create("198.51.100.1"); User staff = resident(); staff.setStaff(true);
        UUID issue = db.queryForObject("SELECT id FROM issues WHERE reported_by_pkid=? AND status='reported'", UUID.class, guest.getPkid());
        together(() -> issues.updateStatus(issue, new IssueStatusUpdateRequest(null, null, IssueStatus.RESOLVED), staff));
        verify(mail, times(1)).send(eq(guest.getEmail()), eq("Issue Resolved"), anyString());
    }

    @Test void concurrentSignedInViewsDeduplicateAndAnonymousCallsDoNotCount() throws Exception {
        User resident = resident(); long post = post(resident);
        together(() -> views.record("post", post, resident, new MockHttpServletRequest()));
        views.record("post", post, null, new MockHttpServletRequest());
        assertThat(count("SELECT count(*) FROM content_views WHERE object_pkid=?", post)).isEqualTo(1);
    }

    private void together(Runnable action) throws Exception {
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> { await(start); action.run(); });
            var b = pool.submit(() -> { await(start); action.run(); });
            start.countDown(); a.get(20,TimeUnit.SECONDS); b.get(20,TimeUnit.SECONDS);
        }
    }
    private void await(CountDownLatch latch) { try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); } }
    private int count(String sql, Object value) { return db.queryForObject(sql, Integer.class, value); }
    private User resident() {
        String name = "resident_" + UUID.randomUUID().toString().replace("-", "");
        long id = db.queryForObject("INSERT INTO users(id,email,username,first_name,last_name,password,is_active) VALUES(?,?,?,'Test','Resident','!test',true) RETURNING pkid", Long.class,
                UUID.randomUUID(), name+"@example.invalid",name);
        db.update("INSERT INTO profiles(id,user_pkid,slug) VALUES(?,?,?)",UUID.randomUUID(),id,name);
        return users.findById(id).orElseThrow();
    }
    private long post(User author) { return db.queryForObject("INSERT INTO posts(id,title,slug,body,author_pkid) VALUES(?,'Post',?,'Body',?) RETURNING pkid", Long.class, UUID.randomUUID(), UUID.randomUUID().toString(), author.getPkid()); }
}
