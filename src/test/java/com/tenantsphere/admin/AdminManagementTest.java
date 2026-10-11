package com.tenantsphere.admin;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenantsphere.auth.AppUserDetails;
import com.tenantsphere.mail.MailService;
import com.tenantsphere.profile.*;
import com.tenantsphere.user.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.*;
import org.springframework.test.context.*;
import org.springframework.transaction.annotation.*;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.file.*;
import java.util.*;

@DataJpaTest(properties = "spring.jpa.show-sql=false")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    AdminQueryService.class,
    AdminRecordService.class,
    AdminDeleteService.class,
    AdminAuditService.class,
    AdminPermissions.class,
    AvatarService.class,
    AvatarStorage.class,
    AdminManagementTest.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AdminManagementTest {
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    static final Path UPLOADS;

    static {
        try {
            UPLOADS = Files.createTempDirectory("ts-admin-test-");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry r) {
        POSTGRES.start();
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("app.media.avatar-dir", UPLOADS::toString);
    }

    @AfterAll
    static void cleanup() throws Exception {
        POSTGRES.stop();
        try (var files = Files.list(UPLOADS)) {
            for (var p : files.toList()) Files.deleteIfExists(p);
        }
        Files.deleteIfExists(UPLOADS);
    }

    @TestConfiguration
    static class Config {
        @Bean
        PasswordEncoder passwordEncoder() {
            return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired AdminRecordService writes;
    @Autowired AdminQueryService queries;
    @Autowired AdminDeleteService deletes;
    @Autowired AdminPermissions access;
    @Autowired AdminAuditService audit;
    @Autowired JdbcTemplate db;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @MockBean MailService mail;
    long admin;

    @BeforeEach
    void prepare() {
        SecurityContextHolder.clearContext();
        db.execute("TRUNCATE users,tags,admin_groups,admin_audit_log CASCADE");
        reset(mail);
        when(mail.siteName()).thenReturn("TenantSphere");
        admin = fixture("admin", true, true, "tenant");
        login(admin);
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void localSetupCreatesAuditedAdministratorWithoutChangingExistingAccounts() {
        String password = "LocalSetupSecret42";
        long created =
                CreateAdminCommand.create(
                        db.getDataSource(),
                        "OPERATOR@example.invalid",
                        "local_operator",
                        "Local",
                        "Operator",
                        password);
        var row = db.queryForMap("SELECT * FROM users WHERE pkid=?", created);
        assertThat(row.get("email")).isEqualTo("operator@example.invalid");
        assertThat(row)
                .containsEntry("is_active", true)
                .containsEntry("is_staff", true)
                .containsEntry("is_superuser", true);
        assertThat(encoder.matches(password, row.get("password").toString())).isTrue();
        assertThat(
                        db.queryForObject(
                                "SELECT COUNT(*) FROM profiles WHERE user_pkid=?",
                                Integer.class,
                                created))
                .isEqualTo(1);
        var history =
                db.queryForList(
                        "SELECT after_data::text FROM admin_audit_log WHERE actor_id=?",
                        String.class,
                        row.get("id"));
        assertThat(history)
                .hasSize(2)
                .allSatisfy(
                        json -> {
                            assertThat(json).doesNotContain(password).doesNotContain("argon2");
                            assertThat(json).contains("local command");
                        });
        int count = db.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertThatThrownBy(
                        () ->
                                CreateAdminCommand.create(
                                        db.getDataSource(),
                                        "operator@example.invalid",
                                        "another_operator",
                                        "Other",
                                        "Operator",
                                        "AnotherSecret42"))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        assertThatThrownBy(
                        () ->
                                CreateAdminCommand.create(
                                        db.getDataSource(),
                                        "another@example.invalid",
                                        "LOCAL_OPERATOR",
                                        "Other",
                                        "Operator",
                                        "AnotherSecret42"))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(count);
        assertThat(
                        db.queryForObject(
                                "SELECT password FROM users WHERE pkid=?", String.class, created))
                .isEqualTo(row.get("password"));
    }

    @Test
    void localSetupRejectsWeakPasswordsAndInvalidIdentityBeforeWriting() {
        int count = db.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertThatThrownBy(
                        () ->
                                CreateAdminCommand.create(
                                        db.getDataSource(),
                                        "new@example.invalid",
                                        "new_admin",
                                        "Local",
                                        "Operator",
                                        "weak"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                CreateAdminCommand.create(
                                        db.getDataSource(),
                                        "invalid",
                                        "new_admin",
                                        "Local",
                                        "Operator",
                                        "LocalSetupSecret42"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                CreateAdminCommand.create(
                                        db.getDataSource(),
                                        "new@example.invalid",
                                        "invalid user",
                                        "Local",
                                        "Operator",
                                        "LocalSetupSecret42"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(count);
    }

    @Test
    void everyRegisteredResourceSupportsCreateEditReadAndDelete() {
        long resident = createUser("resident"), technician = createUser("technician");
        long profile =
                db.queryForObject(
                        "SELECT pkid FROM profiles WHERE user_pkid=?", Long.class, technician);
        var edit = queries.read(AdminResource.PROFILES, profile);
        edit.put("occupation", "plumber");
        edit.put("bio", "Qualified maintenance technician");
        save(AdminResource.PROFILES, profile, edit);
        long apartment =
                writes.save(
                        AdminResource.APARTMENTS,
                        null,
                        null,
                        map(
                                "unit_number",
                                "T101",
                                "building",
                                "North",
                                "floor",
                                2,
                                "tenant_pkid",
                                resident));
        long issue = writes.save(AdminResource.ISSUES, null, null, issue(apartment, resident));
        long post = writes.save(AdminResource.POSTS, null, null, post(resident));
        long rating =
                writes.save(
                        AdminResource.RATINGS,
                        null,
                        null,
                        map(
                                "rated_user_pkid",
                                technician,
                                "rating_user_pkid",
                                resident,
                                "rating",
                                4,
                                "comment",
                                "Arrived on time"));
        long report = writes.save(AdminResource.REPORTS, null, null, report(resident, technician));
        long view =
                writes.save(
                        AdminResource.CONTENT_VIEWS,
                        null,
                        null,
                        map(
                                "content_type",
                                "post",
                                "object_pkid",
                                post,
                                "user_pkid",
                                resident,
                                "viewer_ip",
                                "127.0.0.1",
                                "last_viewed",
                                "2026-09-29T12:00:00Z"));
        long group =
                writes.save(
                        AdminResource.GROUPS,
                        null,
                        null,
                        map(
                                "name",
                                "Maintenance desk",
                                "permissions",
                                List.of("issues.view", "issues.change")));
        for (var pair :
                List.of(
                        new Object[] {AdminResource.APARTMENTS, apartment, "building", "South"},
                        new Object[] {AdminResource.ISSUES, issue, "title", "Replaced valve"},
                        new Object[] {AdminResource.POSTS, post, "title", "Community announcement"},
                        new Object[] {AdminResource.RATINGS, rating, "rating", 5},
                        new Object[] {
                            AdminResource.REPORTS, report, "description", "Reviewed evidence"
                        },
                        new Object[] {AdminResource.CONTENT_VIEWS, view, "viewer_ip", "::1"},
                        new Object[] {AdminResource.GROUPS, group, "name", "Help desk"})) {
            var r = (AdminResource) pair[0];
            long id = (long) pair[1];
            var row = queries.read(r, id);
            row.put(pair[2].toString(), pair[3]);
            save(r, id, row);
            assertThat(queries.read(r, id).get(pair[2].toString()).toString())
                    .isEqualTo(pair[3].toString());
            assertThat(audit.history(r, id, "", 0, 20)).hasSize(2);
        }
        delete(AdminResource.CONTENT_VIEWS, view);
        delete(AdminResource.RATINGS, rating);
        delete(AdminResource.REPORTS, report);
        delete(AdminResource.ISSUES, issue);
        delete(AdminResource.POSTS, post);
        delete(AdminResource.APARTMENTS, apartment);
        delete(AdminResource.GROUPS, group);
        delete(AdminResource.PROFILES, profile);
        assertThat(
                        db.queryForObject(
                                "SELECT is_active FROM users WHERE pkid=?",
                                Boolean.class,
                                technician))
                .isFalse();
        long recreated =
                writes.save(
                        AdminResource.PROFILES,
                        null,
                        null,
                        map(
                                "user_pkid",
                                technician,
                                "gender",
                                "other",
                                "occupation",
                                "plumber",
                                "phone_number",
                                "+919900001111",
                                "country_of_origin",
                                "IN",
                                "city_of_origin",
                                "Delhi",
                                "bio",
                                "Restored"));
        assertThat(recreated).isNotEqualTo(profile);
        delete(AdminResource.USERS, resident);
        delete(AdminResource.USERS, technician);
        assertThat(audit.history(AdminResource.USERS, resident, "", 0, 10))
                .extracting(row -> row.get("action"))
                .contains("add", "delete");
    }

    @Test
    void passwordIsHashedConfirmedAndNeverExposedInQueriesOrHistory() {
        long id = createUser("password_test");
        String hash =
                db.queryForObject("SELECT password FROM users WHERE pkid=?", String.class, id);
        assertThat(encoder.matches("SecurePass9", hash)).isTrue();
        assertThat(queries.read(AdminResource.USERS, id)).doesNotContainKey("password");
        var row = queries.read(AdminResource.USERS, id);
        row.put("password", "NewSecure8");
        row.put("password_confirmation", "Mismatch9");
        assertThatThrownBy(() -> save(AdminResource.USERS, id, row))
                .hasMessageContaining("do not match");
        row.put("password_confirmation", "NewSecure8");
        save(AdminResource.USERS, id, row);
        assertThat(
                        encoder.matches(
                                "NewSecure8",
                                db.queryForObject(
                                        "SELECT password FROM users WHERE pkid=?",
                                        String.class,
                                        id)))
                .isTrue();
        assertThat(audit.history(AdminResource.USERS, id, "", 0, 10).toString())
                .doesNotContain(hash, "NewSecure8", "SecurePass9")
                .contains("password_changed");
    }

    @Test
    void staleEditsCannotOverwriteNewerValues() {
        long id = createUser("stale");
        var stale = queries.read(AdminResource.USERS, id);
        var fresh = new LinkedHashMap<>(stale);
        fresh.put("first_name", "Changed");
        save(AdminResource.USERS, id, fresh);
        stale.put("first_name", "Old");
        assertThatThrownBy(() -> save(AdminResource.USERS, id, stale))
                .hasMessageContaining("changed while");
        assertThat(queries.read(AdminResource.USERS, id).get("first_name")).isEqualTo("Changed");
    }

    @Test
    void groupAndDirectPermissionsAreEnforcedAndRevocationIsImmediate() {
        long staff = fixture("staff", true, false, "tenant");
        long resident = createUser("managed");
        long group =
                writes.save(
                        AdminResource.GROUPS,
                        null,
                        null,
                        map(
                                "name",
                                "Resident managers",
                                "permissions",
                                List.of("users.view", "users.change")));
        var staffRow = queries.read(AdminResource.USERS, staff);
        staffRow.put("groups", List.of(group));
        save(AdminResource.USERS, staff, staffRow);
        login(staff);
        assertThat(access.allowed(AdminResource.USERS, "change")).isTrue();
        assertThat(access.allowed(AdminResource.POSTS, "view")).isFalse();
        var residentRow = queries.read(AdminResource.USERS, resident);
        residentRow.put("first_name", "Managed");
        save(AdminResource.USERS, resident, residentRow);
        assertThatThrownBy(() -> writes.save(AdminResource.POSTS, null, null, post(resident)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
                        () ->
                                writes.save(
                                        AdminResource.GROUPS,
                                        null,
                                        null,
                                        map("name", "Escalation")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
                        () ->
                                save(
                                        AdminResource.USERS,
                                        admin,
                                        queries.read(AdminResource.USERS, admin)))
                .isInstanceOf(AccessDeniedException.class);
        db.update("DELETE FROM admin_user_groups WHERE user_pkid=?", staff);
        assertThatThrownBy(() -> queries.read(AdminResource.USERS, resident))
                .isInstanceOf(AccessDeniedException.class);
        db.update("INSERT INTO admin_user_permissions VALUES(?,?)", staff, "users.view");
        assertThat(queries.read(AdminResource.USERS, resident)).isNotEmpty();
        db.update("UPDATE users SET is_staff=FALSE WHERE pkid=?", staff);
        assertThatThrownBy(() -> queries.read(AdminResource.USERS, resident))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void staffCannotEscalateThroughAUserEditor() {
        long staff = fixture("restricted", true, false, "tenant"),
                resident = createUser("ordinary");
        grant(staff, "users.view", "users.change", "users.add");
        login(staff);
        var row = queries.read(AdminResource.USERS, resident);
        row.put("is_superuser", true);
        row.put("is_staff", true);
        row.put("is_active", false);
        save(AdminResource.USERS, resident, row);
        assertThat(
                        db.queryForObject(
                                "SELECT is_superuser OR is_staff OR NOT is_active FROM users WHERE"
                                        + " pkid=?",
                                Boolean.class,
                                resident))
                .isFalse();
        row = queries.read(AdminResource.USERS, resident);
        row.put("permissions", List.of("users.delete"));
        var input = row;
        assertThatThrownBy(() -> save(AdminResource.USERS, resident, input))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void anonymousAndResidentCannotReadWriteOrPreviewDeletions() {
        long resident = createUser("unprivileged");
        var selection =
                Map.of(
                        resident,
                        (String) queries.read(AdminResource.USERS, resident).get("revision"));
        for (boolean anonymous : List.of(true, false)) {
            if (anonymous) SecurityContextHolder.clearContext();
            else login(resident);
            assertThatThrownBy(
                            () ->
                                    queries.list(
                                            AdminResource.USERS,
                                            AdminQueryService.Query.empty(),
                                            0,
                                            50))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(
                            () ->
                                    writes.save(
                                            AdminResource.APARTMENTS,
                                            null,
                                            null,
                                            map("unit_number", "BAD", "building", "B", "floor", 0)))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> deletes.preview(AdminResource.USERS, selection))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> audit.history(null, null, "", 0, 10))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    void fiveDistinctReportersDeactivateAndDeletingAReportRecalculatesWithoutReactivating() {
        long target = createUser("reported");
        List<Long> reports = new ArrayList<>();
        long first = 0;
        for (int i = 0; i < 5; i++) {
            long reporter = createUser("reporter" + i);
            if (i == 0) first = reporter;
            reports.add(writes.save(AdminResource.REPORTS, null, null, report(reporter, target)));
            if (i < 4)
                assertThat(
                                db.queryForObject(
                                        "SELECT is_active FROM users WHERE pkid=?",
                                        Boolean.class,
                                        target))
                        .isTrue();
        }
        writes.save(AdminResource.REPORTS, null, null, report(first, target));
        assertThat(
                        db.queryForObject(
                                "SELECT report_count FROM profiles WHERE user_pkid=?",
                                Integer.class,
                                target))
                .isEqualTo(5);
        assertThat(
                        db.queryForObject(
                                "SELECT is_active FROM users WHERE pkid=?", Boolean.class, target))
                .isFalse();
        var row = queries.read(AdminResource.USERS, target);
        row.put("is_active", true);
        assertThatThrownBy(() -> save(AdminResource.USERS, target, row))
                .hasMessageContaining("five different");
        delete(AdminResource.REPORTS, reports.getLast());
        assertThat(
                        db.queryForObject(
                                "SELECT report_count FROM profiles WHERE user_pkid=?",
                                Integer.class,
                                target))
                .isEqualTo(4);
        assertThat(
                        db.queryForObject(
                                "SELECT reputation FROM profiles WHERE user_pkid=?",
                                Integer.class,
                                target))
                .isEqualTo(20);
        assertThat(
                        db.queryForObject(
                                "SELECT is_active FROM users WHERE pkid=?", Boolean.class, target))
                .isFalse();
        row.put("revision", queries.read(AdminResource.USERS, target).get("revision"));
        save(AdminResource.USERS, target, row);
        assertThat(
                        db.queryForObject(
                                "SELECT is_active FROM users WHERE pkid=?", Boolean.class, target))
                .isTrue();
        verify(mail, times(1)).send(anyString(), contains("Deactivation"), anyString());
    }

    @Test
    void reportsKeepAStaffAccountOnAndItCanStillBeEdited() {
        long staff = createUser("reported_staff");
        db.update("UPDATE users SET is_staff=TRUE WHERE pkid=?", staff);
        for (int i = 0; i < 5; i++) {
            writes.save(AdminResource.REPORTS, null, null, report(createUser("staff_reporter" + i), staff));
        }
        assertThat(
                        db.queryForObject(
                                "SELECT report_count FROM profiles WHERE user_pkid=?",
                                Integer.class,
                                staff))
                .isEqualTo(5);
        assertThat(
                        db.queryForObject(
                                "SELECT is_active FROM users WHERE pkid=?", Boolean.class, staff))
                .isTrue();

        var row = queries.read(AdminResource.USERS, staff);
        row.put("first_name", "Renamed");
        save(AdminResource.USERS, staff, row);
        row = queries.read(AdminResource.USERS, staff);
        row.put("is_active", false);
        save(AdminResource.USERS, staff, row);
        row = queries.read(AdminResource.USERS, staff);
        row.put("is_active", true);
        save(AdminResource.USERS, staff, row);
        assertThat(
                        db.queryForObject(
                                "SELECT is_active FROM users WHERE pkid=?", Boolean.class, staff))
                .isTrue();
        verify(mail, never()).send(anyString(), contains("Deactivation"), anyString());
    }

    @Test
    void movingAReportUpdatesBothTargetsAndSelfReportingIsRejected() {
        long reporter = createUser("reporter"),
                a = createUser("target_a"),
                b = createUser("target_b");
        long report = writes.save(AdminResource.REPORTS, null, null, report(reporter, a));
        var row = queries.read(AdminResource.REPORTS, report);
        row.put("reported_user_pkid", b);
        save(AdminResource.REPORTS, report, row);
        assertThat(
                        db.queryForObject(
                                "SELECT report_count FROM profiles WHERE user_pkid=?",
                                Integer.class,
                                a))
                .isZero();
        assertThat(
                        db.queryForObject(
                                "SELECT report_count FROM profiles WHERE user_pkid=?",
                                Integer.class,
                                b))
                .isOne();
        assertThatThrownBy(() -> writes.save(AdminResource.REPORTS, null, null, report(a, a)))
                .hasMessageContaining("themselves");
    }

    @Test
    void issueResolutionIsIdempotentReopeningClearsDateAndBulkFailureRollsBack() {
        long resident = createUser("issue_owner");
        long apartment =
                writes.save(
                        AdminResource.APARTMENTS,
                        null,
                        null,
                        map("unit_number", "I1", "building", "B", "floor", 1));
        long issue = writes.save(AdminResource.ISSUES, null, null, issue(apartment, resident));
        reset(mail);
        when(mail.siteName()).thenReturn("TenantSphere");
        var row = queries.read(AdminResource.ISSUES, issue);
        row.put("status", "resolved");
        save(AdminResource.ISSUES, issue, row);
        assertThat(queries.read(AdminResource.ISSUES, issue).get("resolved_on")).isNotNull();
        verify(mail, times(1)).send(anyString(), eq("Issue Resolved"), anyString());
        row = queries.read(AdminResource.ISSUES, issue);
        save(AdminResource.ISSUES, issue, row);
        verify(mail, times(1)).send(anyString(), eq("Issue Resolved"), anyString());
        row = queries.read(AdminResource.ISSUES, issue);
        row.put("status", "reported");
        save(AdminResource.ISSUES, issue, row);
        assertThat(queries.read(AdminResource.ISSUES, issue).get("resolved_on")).isNull();
        long second = writes.save(AdminResource.ISSUES, null, null, issue(apartment, resident));
        reset(mail);
        var selection = Map.of(issue, rev(AdminResource.ISSUES, issue), second, "stale");
        assertThatThrownBy(() -> writes.bulkIssueStatus(selection, "resolved"))
                .hasMessageContaining("changed while");
        assertThat(queries.read(AdminResource.ISSUES, issue).get("status")).isEqualTo("reported");
        verify(mail, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void deletionPreviewIncludesCascadesAndRequiresTheirPermissions() {
        long resident = createUser("cascade"), staff = fixture("staff", true, false, "tenant");
        long apartment =
                writes.save(
                        AdminResource.APARTMENTS,
                        null,
                        null,
                        map("unit_number", "C1", "building", "B", "floor", 0));
        long issue = writes.save(AdminResource.ISSUES, null, null, issue(apartment, resident));
        var selected = Map.of(apartment, rev(AdminResource.APARTMENTS, apartment));
        var preview = deletes.preview(AdminResource.APARTMENTS, selected);
        assertThat(preview.counts()).containsEntry("Apartments", 1).containsEntry("Issues", 1);
        grant(staff, "apartments.view", "apartments.delete");
        login(staff);
        assertThatThrownBy(() -> deletes.preview(AdminResource.APARTMENTS, selected))
                .isInstanceOf(AccessDeniedException.class);
        login(admin);
        deletes.delete(AdminResource.APARTMENTS, selected, preview.fingerprint());
        assertThat(
                        db.queryForObject(
                                "SELECT COUNT(*) FROM issues WHERE pkid=?", Integer.class, issue))
                .isZero();
        assertThat(audit.history(AdminResource.ISSUES, issue, "", 0, 10))
                .extracting(r -> r.get("action"))
                .contains("cascade_delete");
    }

    @Test
    void changedDeletionPreviewMustBeReviewedAgain() {
        long resident = createUser("new_child");
        long apartment =
                writes.save(
                        AdminResource.APARTMENTS,
                        null,
                        null,
                        map("unit_number", "D1", "building", "B", "floor", 0));
        var selected = Map.of(apartment, rev(AdminResource.APARTMENTS, apartment));
        var preview = deletes.preview(AdminResource.APARTMENTS, selected);
        writes.save(AdminResource.ISSUES, null, null, issue(apartment, resident));
        assertThatThrownBy(
                        () ->
                                deletes.delete(
                                        AdminResource.APARTMENTS, selected, preview.fingerprint()))
                .hasMessageContaining("Review the deletion");
    }

    @Test
    void accountDeletionRecountsVotesAndReportsAndRemovesOrphanViews() {
        long owner = createUser("post_owner"), voter = createUser("voter");
        long post = writes.save(AdminResource.POSTS, null, null, post(owner));
        db.update("INSERT INTO post_upvotes VALUES(?,?)", post, voter);
        db.update("UPDATE posts SET upvotes=1 WHERE pkid=?", post);
        writes.save(AdminResource.REPORTS, null, null, report(voter, owner));
        delete(AdminResource.USERS, voter);
        assertThat(db.queryForObject("SELECT upvotes FROM posts WHERE pkid=?", Integer.class, post))
                .isZero();
        assertThat(
                        db.queryForObject(
                                "SELECT report_count FROM profiles WHERE user_pkid=?",
                                Integer.class,
                                owner))
                .isZero();
        writes.save(
                AdminResource.CONTENT_VIEWS,
                null,
                null,
                map(
                        "content_type",
                        "post",
                        "object_pkid",
                        post,
                        "viewer_ip",
                        "127.0.0.1",
                        "last_viewed",
                        "2026-09-29T12:00:00Z"));
        delete(AdminResource.USERS, owner);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM content_views", Integer.class)).isZero();
    }

    @Test
    void administratorCannotDeleteOrDemoteTheirOwnAccount() {
        assertThatThrownBy(() -> delete(AdminResource.USERS, admin))
                .hasMessageContaining("own account");
        var row = queries.read(AdminResource.USERS, admin);
        row.put("is_superuser", false);
        assertThatThrownBy(() -> save(AdminResource.USERS, admin, row))
                .hasMessageContaining("own administration access");
    }

    @Test
    void invalidRatingsAndContentReferencesAreRejected() {
        long a = createUser("a"), b = createUser("b");
        assertThatThrownBy(
                        () ->
                                writes.save(
                                        AdminResource.RATINGS,
                                        null,
                                        null,
                                        map(
                                                "rated_user_pkid",
                                                a,
                                                "rating_user_pkid",
                                                b,
                                                "rating",
                                                5,
                                                "comment",
                                                "Invalid")))
                .hasMessageContaining("from a resident to a technician");
        long technician = fixture("rating_plumber", false, false, "plumber");
        assertThatThrownBy(
                        () ->
                                writes.save(
                                        AdminResource.RATINGS,
                                        null,
                                        null,
                                        map(
                                                "rated_user_pkid",
                                                a,
                                                "rating_user_pkid",
                                                technician,
                                                "rating",
                                                5,
                                                "comment",
                                                "Invalid")))
                .hasMessageContaining("from a resident to a technician");
        assertThatThrownBy(
                        () ->
                                writes.save(
                                        AdminResource.CONTENT_VIEWS,
                                        null,
                                        null,
                                        map(
                                                "content_type",
                                                "post",
                                                "object_pkid",
                                                99999,
                                                "viewer_ip",
                                                "127.0.0.1",
                                                "last_viewed",
                                                "2026-09-29T12:00:00Z")))
                .hasMessageContaining("no longer exists");
    }

    @Test
    void searchFiltersSortingAndPaginationOperateOnTheDatabase() {
        for (int i = 0; i < 6; i++)
            writes.save(
                    AdminResource.APARTMENTS,
                    null,
                    null,
                    map(
                            "unit_number",
                            "F" + i,
                            "building",
                            i < 4 ? "North" : "South",
                            "floor",
                            i,
                            "tenant_pkid",
                            admin));
        var q = new AdminQueryService.Query("North", Map.of(), null, null, "floor", false);
        assertThat(queries.count(AdminResource.APARTMENTS, q)).isEqualTo(4);
        assertThat(queries.list(AdminResource.APARTMENTS, q, 1, 2))
                .extracting(r -> r.get("floor"))
                .containsExactly(1, 2);
        q = new AdminQueryService.Query("", Map.of("floor", 5), null, null, "pkid", false);
        assertThat(queries.count(AdminResource.APARTMENTS, q)).isOne();
        assertThat(
                        queries.count(
                                AdminResource.APARTMENTS,
                                new AdminQueryService.Query(
                                        "' OR TRUE --", Map.of(), null, null, "pkid", false)))
                .isZero();
    }

    @Test
    void historyIsAppendOnlyAndSurvivesAccountDeletion() {
        long id = createUser("history");
        delete(AdminResource.USERS, id);
        assertThat(audit.history(AdminResource.USERS, id, "", 0, 10)).hasSize(2);
        assertThatThrownBy(() -> db.update("DELETE FROM admin_audit_log WHERE resource='users'"))
                .hasMessageContaining("append-only");
    }

    @Test
    void avatarUploadUsesImageValidationAndIsAudited() {
        long id = createUser("avatar");
        long profile =
                db.queryForObject("SELECT pkid FROM profiles WHERE user_pkid=?", Long.class, id);
        var bad =
                new MockMultipartFile("avatar", "bad.png", "image/png", "not an image".getBytes());
        assertThatThrownBy(
                        () ->
                                writes.uploadAvatar(
                                        profile, rev(AdminResource.PROFILES, profile), bad))
                .hasMessageContaining("valid JPEG");
        var image =
                new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes = new java.io.ByteArrayOutputStream();
        try {
            javax.imageio.ImageIO.write(image, "png", bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        writes.uploadAvatar(
                profile,
                rev(AdminResource.PROFILES, profile),
                new MockMultipartFile("avatar", "valid.png", "image/png", bytes.toByteArray()));
        assertThat(queries.read(AdminResource.PROFILES, profile).get("avatar").toString())
                .startsWith("/media/avatars/");
        assertThat(audit.history(AdminResource.PROFILES, profile, "", 0, 10))
                .extracting(r -> r.get("action"))
                .contains("avatar");
        String reference = queries.read(AdminResource.PROFILES, profile).get("avatar").toString();
        Path file = UPLOADS.resolve(reference.substring(reference.lastIndexOf('/') + 1));
        assertThat(file).exists();
        writes.clearAvatar(profile, rev(AdminResource.PROFILES, profile));
        assertThat(file).doesNotExist();
        assertThat(queries.read(AdminResource.PROFILES, profile).get("avatar")).isNull();
    }

    @Test
    void tagsCanBeManagedWithoutDeletingPosts() {
        long resident = createUser("tag_owner");
        long post = writes.save(AdminResource.POSTS, null, null, post(resident));
        long tag = writes.save(AdminResource.TAGS, null, null, map("name", "garden"));
        var row = queries.read(AdminResource.TAGS, tag);
        row.put("name", "gardening");
        save(AdminResource.TAGS, tag, row);
        db.update("INSERT INTO post_tags VALUES(?,?)", post, tag);
        assertThat(queries.read(AdminResource.TAGS, tag).get("post_count")).isEqualTo(1L);
        delete(AdminResource.TAGS, tag);
        assertThat(
                        db.queryForObject(
                                "SELECT COUNT(*) FROM posts WHERE pkid=?", Integer.class, post))
                .isOne();
        assertThat(
                        db.queryForObject(
                                "SELECT COUNT(*) FROM post_tags WHERE tag_pkid=?",
                                Integer.class,
                                tag))
                .isZero();
    }

    @Test
    void relationPickersRequireViewAccessAndHideLabelsOtherwise() {
        long staff = fixture("rater", true, false, "tenant"), resident = createUser("neighbour");
        grant(staff, "ratings.change");
        login(staff);
        assertThatThrownBy(() -> queries.choices("users", "", 0, 20))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(queries.choice("users", resident).label()).isEqualTo("#" + resident);
        assertThatThrownBy(() -> queries.choice("tags", 1))
                .isInstanceOf(IllegalArgumentException.class);
        grant(staff, "users.view");
        assertThat(queries.choices("users", "neighbour", 0, 20))
                .extracting(AdminQueryService.Choice::label)
                .anyMatch(label -> label.contains("neighbour"));
        assertThat(queries.choice("users", resident).label()).contains("neighbour");
    }

    @Test
    void postTagsReuseCaseVariantsOfExistingTags() {
        long resident = createUser("tagger");
        long existing = writes.save(AdminResource.TAGS, null, null, map("name", "plumbing"));
        var data = post(resident);
        data.put("tags", "Plumbing, PLUMBING, garden");
        long post = writes.save(AdminResource.POSTS, null, null, data);
        assertThat(
                        db.queryForObject(
                                "SELECT COUNT(*) FROM tags WHERE LOWER(name)='plumbing'",
                                Integer.class))
                .isOne();
        assertThat(
                        db.queryForList(
                                "SELECT tag_pkid FROM post_tags WHERE post_pkid=?", Long.class, post))
                .hasSize(2)
                .contains(existing);
    }

    @Test
    void historySearchTreatsWildcardsLiterally() {
        writes.save(AdminResource.TAGS, null, null, map("name", "a_b"));
        writes.save(AdminResource.TAGS, null, null, map("name", "axb"));
        assertThat(audit.history(AdminResource.TAGS, null, "a_b", 0, 20))
                .extracting(row -> row.get("object_label"))
                .containsOnly("a_b");
    }

    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;

    @Test
    void deletingAVoterDoesNotLoseAnotherConcurrentVote() throws Exception {
        long owner = createUser("concurrent_owner");
        long removed = createUser("removed_voter");
        long remaining = createUser("remaining_voter");
        long post = writes.save(AdminResource.POSTS, null, null, post(owner));
        db.update("INSERT INTO post_upvotes VALUES(?,?)", post, removed);
        db.update("UPDATE posts SET upvotes=1 WHERE pkid=?", post);
        var selection = Map.of(removed, rev(AdminResource.USERS, removed));
        var preview = deletes.preview(AdminResource.USERS, selection);
        var held = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var voting =
                java.util.concurrent.CompletableFuture.runAsync(
                        () ->
                                new org.springframework.transaction.support.TransactionTemplate(
                                                transactions)
                                        .executeWithoutResult(
                                                tx -> {
                                                    db.queryForList(
                                                            "SELECT pkid FROM posts WHERE pkid=?"
                                                                    + " FOR UPDATE",
                                                            post);
                                                    db.update(
                                                            "INSERT INTO post_upvotes VALUES(?,?)",
                                                            post,
                                                            remaining);
                                                    db.update(
                                                            "UPDATE posts SET upvotes=2 WHERE"
                                                                    + " pkid=?",
                                                            post);
                                                    held.countDown();
                                                    try {
                                                        if (!release.await(
                                                                10,
                                                                java.util.concurrent.TimeUnit
                                                                        .SECONDS))
                                                            throw new IllegalStateException(
                                                                    "No release");
                                                    } catch (InterruptedException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                }));
        assertThat(held.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        var deletion =
                java.util.concurrent.CompletableFuture.runAsync(
                        () -> {
                            login(admin);
                            try {
                                deletes.delete(
                                        AdminResource.USERS, selection, preview.fingerprint());
                            } finally {
                                SecurityContextHolder.clearContext();
                            }
                        });
        try {
            for (int i = 0; i < 100; i++) {
                if (db.queryForObject(
                                "SELECT count(*) FROM pg_stat_activity WHERE"
                                        + " datname=current_database() AND wait_event_type='Lock'",
                                Integer.class)
                        > 0) break;
                Thread.sleep(20);
            }
        } finally {
            release.countDown();
        }
        voting.get(10, java.util.concurrent.TimeUnit.SECONDS);
        deletion.get(10, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(db.queryForObject("SELECT upvotes FROM posts WHERE pkid=?", Integer.class, post))
                .isOne();
        assertThat(
                        db.queryForObject(
                                "SELECT user_pkid FROM post_upvotes WHERE post_pkid=?",
                                Long.class,
                                post))
                .isEqualTo(remaining);
    }

    private long createUser(String username) {
        return writes.save(
                AdminResource.USERS,
                null,
                null,
                map(
                        "email",
                        username + "@example.invalid",
                        "username",
                        username,
                        "first_name",
                        "Test",
                        "last_name",
                        "Resident",
                        "password",
                        "SecurePass9",
                        "password_confirmation",
                        "SecurePass9",
                        "is_active",
                        true,
                        "is_staff",
                        false,
                        "is_superuser",
                        false));
    }

    private long fixture(String username, boolean staff, boolean superuser, String occupation) {
        UUID id = UUID.randomUUID();
        long pk =
                db.queryForObject(
                        "INSERT INTO"
                            + " users(id,email,username,first_name,last_name,password,is_active,is_staff,is_superuser)"
                            + " VALUES(?,?,?,'Test','Resident','fixture',true,?,?) RETURNING pkid",
                        Long.class,
                        id,
                        username + "@example.invalid",
                        username,
                        staff,
                        superuser);
        db.update(
                "INSERT INTO profiles(id,user_pkid,slug,occupation) VALUES(?,?,?,?)",
                UUID.randomUUID(),
                pk,
                username,
                occupation);
        return pk;
    }

    private void login(long id) {
        var principal = new AppUserDetails(users.findById(id).orElseThrow());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()));
    }

    private void grant(long staff, String... permissions) {
        for (String p : permissions)
            db.update("INSERT INTO admin_user_permissions VALUES(?,?)", staff, p);
    }

    private String rev(AdminResource r, long id) {
        return (String) queries.read(r, id).get("revision");
    }

    private void save(AdminResource r, long id, Map<String, Object> data) {
        writes.save(r, id, (String) data.get("revision"), data);
    }

    private void delete(AdminResource r, long id) {
        var selected = Map.of(id, rev(r, id));
        var preview = deletes.preview(r, selected);
        deletes.delete(r, selected, preview.fingerprint());
    }

    private static Map<String, Object> issue(long apartment, long reporter) {
        return map(
                "title",
                "Leaking tap",
                "description",
                "A steady drip",
                "apartment_pkid",
                apartment,
                "reported_by_pkid",
                reporter,
                "status",
                "reported",
                "priority",
                "low");
    }

    private static Map<String, Object> post(long author) {
        return map(
                "title",
                "Community notice",
                "body",
                "Please use the west entrance.",
                "author_pkid",
                author,
                "tags",
                "maintenance, residents");
    }

    private static Map<String, Object> report(long reporter, long target) {
        return map(
                "title",
                "Noise after quiet hours",
                "description",
                "Repeated loud music",
                "reported_by_pkid",
                reporter,
                "reported_user_pkid",
                target);
    }

    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) out.put(pairs[i].toString(), pairs[i + 1]);
        return out;
    }
}
