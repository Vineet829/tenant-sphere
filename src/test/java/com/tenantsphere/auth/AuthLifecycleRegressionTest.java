package com.tenantsphere.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tenantsphere.config.AppProperties;
import com.tenantsphere.profile.Profile;
import com.tenantsphere.profile.ProfileRepository;
import com.tenantsphere.user.User;
import com.tenantsphere.user.UserRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntPredicate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = "spring.jpa.show-sql=false")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({VerificationTokenService.class, RegistrationService.class, PasswordResetService.class,
        AuthLifecycleRegressionTest.Configuration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuthLifecycleRegressionTest {
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

    @TestConfiguration
    static class Configuration {
        @Bean
        AppProperties appProperties() {
            return new AppProperties(null, null, null,
                    new AppProperties.Verification(Duration.ofDays(3), Duration.ofDays(1)));
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        }
    }

    @Autowired private VerificationTokenService tokens;
    @Autowired private RegistrationService registration;
    @Autowired private PasswordResetService passwords;
    @Autowired private UserRepository users;
    @Autowired private ProfileRepository profiles;
    @Autowired private JdbcTemplate database;
    @Autowired private PasswordEncoder encoder;
    @Autowired private PlatformTransactionManager transactions;
    @MockBean private VerificationMailer mail;

    @BeforeEach
    void clearFixtures() {
        database.execute("TRUNCATE users CASCADE");
        reset(mail);
    }

    @Test
    void concurrentActivationHasExactlyOneWinner() throws Exception {
        User user = user(false);
        String token = tokens.issue(user, VerificationPurpose.ACTIVATION, Duration.ofDays(1));
        assertThat(together(8, i -> registration.activate(tokens.encodeUid(user), token)))
                .filteredOn(Boolean::booleanValue).hasSize(1);
        assertThat(users.findById(user.getPkid()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void concurrentPasswordResetHasExactlyOneWinnerAndOneNotice() throws Exception {
        User user = user(true);
        String token = tokens.issue(user, VerificationPurpose.PASSWORD_RESET, Duration.ofDays(1));
        List<Boolean> outcomes = together(8, i -> {
            try {
                passwords.confirmReset(tokens.encodeUid(user), token, "NewPassword9!", "NewPassword9!");
                return true;
            } catch (InvalidPasswordResetTokenException expected) {
                return false;
            }
        });
        assertThat(outcomes).filteredOn(Boolean::booleanValue).hasSize(1);
        assertThat(encoder.matches("NewPassword9!", users.findById(user.getPkid()).orElseThrow().getPassword())).isTrue();
        verify(mail, times(1)).sendPasswordChanged(any(User.class));
    }

    @Test
    void deactivatedAccountCannotRequestAnotherActivation() {
        User user = user(false);
        ban(user);
        registration.resendActivation(user.getEmail());
        assertThat(database.queryForObject("SELECT count(*) FROM verification_tokens", Integer.class)).isZero();
        verifyNoInteractions(mail);
    }

    @Test
    void tokenIssuedBeforeModerationCannotReactivateAccount() {
        User user = user(false);
        String token = tokens.issue(user, VerificationPurpose.ACTIVATION, Duration.ofDays(1));
        ban(user);
        assertThat(registration.activate(tokens.encodeUid(user), token)).isFalse();
        assertThat(users.findById(user.getPkid()).orElseThrow().isActive()).isFalse();
    }

    @Test
    void invalidPurposeUidAndExpiryCannotConsumeToken() {
        User user = user(false);
        User other = user(false);
        String token = tokens.issue(user, VerificationPurpose.ACTIVATION, Duration.ofDays(1));
        assertThat(tokens.consume(tokens.encodeUid(other), token, VerificationPurpose.ACTIVATION)).isEmpty();
        assertThat(tokens.consume(tokens.encodeUid(user), token, VerificationPurpose.PASSWORD_RESET)).isEmpty();
        assertThat(tokens.consume("invalid-uid", token, VerificationPurpose.ACTIVATION)).isEmpty();
        assertThat(registration.activate(tokens.encodeUid(user), token)).isTrue();
        String expired = tokens.issue(other, VerificationPurpose.ACTIVATION, Duration.ofSeconds(-1));
        assertThat(registration.activate(tokens.encodeUid(other), expired)).isFalse();
    }

    @Test
    void rollbackKeepsResetTokenUsableAndPreservesPassword() {
        User user = user(true);
        String oldHash = user.getPassword();
        String token = tokens.issue(user, VerificationPurpose.PASSWORD_RESET, Duration.ofDays(1));
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            passwords.confirmReset(tokens.encodeUid(user), token, "NewPassword9!", "NewPassword9!");
            status.setRollbackOnly();
        });
        assertThat(users.findById(user.getPkid()).orElseThrow().getPassword()).isEqualTo(oldHash);
        verifyNoInteractions(mail);
        passwords.confirmReset(tokens.encodeUid(user), token, "NewPassword9!", "NewPassword9!");
        assertThat(encoder.matches("NewPassword9!", users.findById(user.getPkid()).orElseThrow().getPassword())).isTrue();
    }

    @Test
    void concurrentResendsLeaveOnlyOneUsableToken() throws Exception {
        User user = user(false);
        together(6, i -> {
            registration.resendActivation(user.getEmail());
            return true;
        });
        assertThat(database.queryForObject("SELECT count(*) FROM verification_tokens", Integer.class)).isEqualTo(1);
        verify(mail, times(6)).sendActivation(any(User.class), anyString(), anyString());
    }

    @Test
    void databaseEnforcesCaseInsensitiveIdentity() {
        User first = user(true);
        User second = user(true);
        assertThatThrownBy(() -> database.update("UPDATE users SET username = ? WHERE pkid = ?",
                first.getUsername().toUpperCase(), second.getPkid())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> database.update("UPDATE users SET email = ? WHERE pkid = ?",
                first.getEmail().toUpperCase(), second.getPkid())).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void ban(User user) {
        database.update("UPDATE profiles SET report_count = 5, reputation = 0 WHERE user_pkid = ?", user.getPkid());
        database.update("UPDATE users SET is_active = FALSE WHERE pkid = ?", user.getPkid());
    }

    private User user(boolean active) {
        return new TransactionTemplate(transactions).execute(status -> {
            User user = new User();
            user.setUsername("qa_" + UUID.randomUUID().toString().replace("-", ""));
            user.setEmail(user.getUsername() + "@example.invalid");
            user.setFirstName("QA");
            user.setLastName("Resident");
            user.setPassword(encoder.encode("Original9!"));
            user.setActive(active);
            users.save(user);
            Profile profile = new Profile();
            profile.setUser(user);
            profile.setSlug(user.getUsername());
            profiles.save(profile);
            return user;
        });
    }

    private List<Boolean> together(int count, IntPredicate task) throws Exception {
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(count)) {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                final int index = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
                    return task.test(index);
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) results.add(future.get(30, TimeUnit.SECONDS));
            return results;
        }
    }
}
