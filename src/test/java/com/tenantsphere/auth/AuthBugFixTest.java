package com.tenantsphere.auth;

import com.tenantsphere.profile.Profile;
import com.tenantsphere.profile.ProfileRepository;
import com.tenantsphere.user.User;
import com.tenantsphere.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.mockito.InOrder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AuthBugFixTest {
    private final UserRepository users = mock(UserRepository.class);
    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final GoogleIdentityProvider google = mock(GoogleIdentityProvider.class);
    private final GoogleIdentityRepository identities = mock(GoogleIdentityRepository.class);
    private final PendingSignUps pending = mock(PendingSignUps.class);
    private final OAuthStateService states = new OAuthStateService();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final String redirect = "https://tenantsphere.example/google";
    private final GoogleOAuthService oauth = new GoogleOAuthService(
            new GoogleOAuthProperties("client", "secret", List.of(redirect)), states, google, users, profiles, passwords, identities,
            pending);

    @Test void inactiveAccountsStillCheckThePassword() {
        User user = user(); user.setActive(false);
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(users.findById(user.getPkid())).thenReturn(Optional.of(user));
        when(passwords.matches("supplied", user.getPassword())).thenReturn(true);
        assertThatThrownBy(() -> auth(passwords).authenticate(user.getEmail(), "supplied"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(passwords).matches("supplied", user.getPassword());
    }

    @Test void bannedActiveAccountsCannotLogInOrRefresh() {
        User user = user(); Profile profile = new Profile(); profile.setReportCount(5);
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(users.findByExternalId(user.getId())).thenReturn(Optional.of(user));
        when(users.findById(user.getPkid())).thenReturn(Optional.of(user));
        when(profiles.findByUserPkid(user.getPkid())).thenReturn(Optional.of(profile));
        when(passwords.matches("supplied", user.getPassword())).thenReturn(true);
        var auth = auth(passwords);
        assertThatThrownBy(() -> auth.authenticate(user.getEmail(), "supplied")).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> auth.requireActiveUser(user.getId())).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test void googleNeverLinksAnExistingPasswordAccountByEmail() {
        when(google.exchange("code", redirect)).thenReturn(info("subject"));
        when(users.existsByEmailIgnoreCase("person@example.com")).thenReturn(true);
        String state = states.issue(redirect, request);
        assertThatThrownBy(() -> oauth.login("google-oauth2", state, "code", request))
                .isInstanceOf(GoogleAuthenticationException.class).hasMessageContaining("password");
        verify(users, never()).save(any()); verify(identities, never()).save(any());
    }

    @Test void googleUsesStableSubjectAndRejectsDeactivatedOrBannedAccounts() {
        User user = user();
        var identity = new GoogleIdentity(); identity.setSubject("subject"); identity.setUser(user);
        when(identities.findById("subject")).thenReturn(Optional.of(identity));
        when(google.exchange("code", redirect)).thenReturn(info("subject"));
        assertThat(oauth.login("google-oauth2", states.issue(redirect, request), "code", request)).isSameAs(user);
        verify(users, never()).existsByEmailIgnoreCase(anyString());
        user.setActive(false);
        assertThatThrownBy(() -> oauth.login("google-oauth2", states.issue(redirect, request), "code", request))
                .isInstanceOf(GoogleAuthenticationException.class);
        user.setActive(true); Profile profile = new Profile(); profile.setReportCount(5);
        when(profiles.findByUserPkid(user.getPkid())).thenReturn(Optional.of(profile));
        assertThatThrownBy(() -> oauth.login("google-oauth2", states.issue(redirect, request), "code", request))
                .isInstanceOf(GoogleAuthenticationException.class);
    }

    @Test void googleSignInClearsAPendingSignUpWithItsEmailFirst() {
        when(google.exchange("code", redirect)).thenReturn(info("new-subject"));
        when(passwords.encode(anyString())).thenReturn("unusable-random-password");
        when(users.save(any())).thenAnswer(call -> { User user = call.getArgument(0); user.setPkid(5L); return user; });
        oauth.login("google-oauth2", states.issue(redirect, request), "code", request);
        InOrder order = inOrder(pending, users);
        order.verify(pending).removeFor("person@example.com");
        order.verify(users).existsByEmailIgnoreCase("person@example.com");
    }

    @Test void newGoogleAccountsKeepAnExplicitSubjectMapping() {
        when(google.exchange("code", redirect)).thenReturn(info("new-subject"));
        when(passwords.encode(anyString())).thenReturn("unusable-random-password");
        when(users.save(any())).thenAnswer(call -> { User user = call.getArgument(0); user.setPkid(4L); return user; });
        User created = oauth.login("google-oauth2", states.issue(redirect, request), "code", request);
        assertThat(created.isActive()).isTrue();
        verify(identities).save(argThat(identity -> identity.getSubject().equals("new-subject") && identity.getUser() == created));
    }

    @Test void stateBelongsToOneBrowserAndCanOnlyBeConsumedOnce() {
        String state = states.issue(redirect, request);
        assertThat(states.consume(state, new MockHttpServletRequest())).isEmpty();
        assertThat(states.consume("forged", request)).isEmpty();
        assertThat(states.consume(state, request)).contains(redirect);
        assertThat(states.consume(state, request)).isEmpty();
    }

    @Test void expiredGuestSessionsCannotRefreshOrReenterUsingAPassword() {
        User user = user(); user.setGuest(true); user.setGuestExpiresAt(Instant.now().minusSeconds(1));
        when(users.findByExternalId(user.getId())).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> auth(passwords).requireActiveUser(user.getId()))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(user.isSessionEligible()).isFalse();
        var realEncoder = org.springframework.security.crypto.argon2.Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> auth(realEncoder).authenticate(user.getEmail(), "attempt"))
                .isInstanceOf(InvalidCredentialsException.class);
        user.setGuestExpiresAt(Instant.now().plusSeconds(300));
        assertThat(user.isSessionEligible()).isTrue();
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(passwords.matches("supplied", user.getPassword())).thenReturn(true);
        assertThatThrownBy(() -> auth(passwords).authenticate(user.getEmail(), "supplied"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test void thePasswordIsCheckedBeforeATransactionOpens() {
        User user = user();
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(users.findById(user.getPkid())).thenReturn(Optional.of(user));
        when(passwords.matches("supplied", user.getPassword())).thenReturn(true);
        assertThat(auth(passwords).authenticate(user.getEmail(), "supplied")).isSameAs(user);
        assertThat(user.getLastLogin()).isNotNull();
        InOrder order = inOrder(passwords, transactions, users);
        order.verify(passwords).matches("supplied", user.getPassword());
        order.verify(transactions).getTransaction(any());
        order.verify(users).findById(user.getPkid());
        order.verify(transactions).commit(any());
    }

    @Test void aWrongPasswordOpensNoTransactionAndAChangedOneSignsInAfresh() {
        User user = user();
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> auth(passwords).authenticate(user.getEmail(), "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(transactions, never()).getTransaction(any());

        User changed = user(); changed.setPassword("new-hash");
        when(users.findById(user.getPkid())).thenReturn(Optional.of(changed));
        when(passwords.matches("supplied", user.getPassword())).thenReturn(true);
        assertThatThrownBy(() -> auth(passwords).authenticate(user.getEmail(), "supplied"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    private AuthService auth(PasswordEncoder encoder) {
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        return new AuthService(users, profiles, encoder, transactions);
    }

    private GoogleUserInfo info(String sub) { return new GoogleUserInfo("person@example.com", "Person", "Resident", sub, true); }
    private User user() {
        User user = new User(); user.setPkid(1L); user.setId(UUID.randomUUID()); user.setEmail("person@example.com");
        user.setUsername("person"); user.setFirstName("Person"); user.setLastName("Resident"); user.setPassword("hash"); user.setActive(true);
        return user;
    }
}
