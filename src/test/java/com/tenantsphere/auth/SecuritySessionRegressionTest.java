package com.tenantsphere.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tenantsphere.common.ApiAccessDeniedHandler;
import com.tenantsphere.common.ApiAuthenticationEntryPoint;
import com.tenantsphere.config.SecurityConfig;
import com.tenantsphere.profile.Profile;
import com.tenantsphere.user.User;
import com.tenantsphere.user.UserRepository;
import com.vaadin.flow.spring.security.RequestUtil;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@WebMvcTest(
        controllers = AuthController.class,
        properties = {
            "SIGNING_KEY=session-test-key-not-used-by-mocked-jwt-service",
            "COOKIE_SECURE=false",
            "vaadin.productionMode=true",
            "spring.devtools.livereload.enabled=false"
        })
@Import({
    SecurityConfig.class,
    JwtCookieAuthenticationFilter.class,
    CookieService.class,
    ApiAccessDeniedHandler.class,
    com.tenantsphere.common.ErrorResponseWriter.class,
    ApiAuthenticationEntryPoint.class
})
class SecuritySessionRegressionTest {
    @Autowired MockMvc mvc;
    @MockBean AuthService auth;
    @MockBean JwtService jwt;
    @MockBean UserRepository users;
    @MockBean com.tenantsphere.profile.ProfileRepository profiles;
    @MockBean RegistrationService registration;
    @MockBean PasswordResetService passwordReset;
    @MockBean RequestLimiter limiter;
    @MockBean TokenRevocations revocations;
    @MockBean RequestUtil requestUtil;
    @MockBean com.tenantsphere.common.HandlerLabelResolver labels;
    @MockBean org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @BeforeEach
    void identity() {
        var user = new User();
        user.setId(UUID.randomUUID());
        user.setPkid(1L);
        user.setEmail("admin@example.invalid");
        user.setUsername("admin");
        user.setFirstName("Admin");
        user.setLastName("Operator");
        user.setActive(true);
        user.setStaff(true);
        var profile = new Profile();
        profile.setUser(user);
        profile.setSlug("admin");
        when(auth.authenticate(anyString(), anyString())).thenReturn(user);
        when(auth.requireProfile(any())).thenReturn(profile);
        when(jwt.issueAccessToken(any(), any())).thenReturn("valid-access");
        when(jwt.issueRefreshToken(any(), any())).thenReturn("valid-refresh");
        when(jwt.extractUserId("valid-access", TokenType.ACCESS)).thenReturn(user.getId());
        when(jwt.isCurrent(eq("valid-access"), any())).thenReturn(true);
        when(users.findByExternalId(user.getId())).thenReturn(Optional.of(user));
    }

    @Test
    void jwtRequestsKeepExistingUiSessionIdStable() throws Exception {
        var session = new MockHttpSession();
        String before = session.getId();
        for (int i = 0; i < 3; i++) {
            mvc.perform(
                            get("/api/v1/auth/users/me/")
                                    .session(session)
                                    .cookie(new Cookie("access", "valid-access")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isString())
                    .andExpect(jsonPath("$.is_staff").value(true))
                    .andExpect(jsonPath("$.is_guest").value(false));
            assertThat(session.getId()).isEqualTo(before);
        }
    }

    @Test
    void passwordLoginRotatesAnExistingUiSession() throws Exception {
        var session = new MockHttpSession();
        String before = session.getId();
        mvc.perform(
                        post("/api/v1/auth/login/").cookie(new Cookie("XSRF-TOKEN", "csrf-fixture")).header("X-XSRF-TOKEN", "csrf-fixture")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"admin@example.invalid\",\"password\":\"ValidSecret42\"}"))
                .andExpect(status().isOk());
        assertThat(session.getId()).isNotEqualTo(before);
    }

    @Test
    void apiOnlyLoginDoesNotCreateAnHttpSession() throws Exception {
        var result =
                mvc.perform(
                                post("/api/v1/auth/login/").cookie(new Cookie("XSRF-TOKEN", "csrf-fixture")).header("X-XSRF-TOKEN", "csrf-fixture")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"email\":\"admin@example.invalid\",\"password\":\"ValidSecret42\"}"))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void failedLoginDoesNotRotateExistingSession() throws Exception {
        when(auth.authenticate(anyString(), anyString()))
                .thenThrow(new InvalidCredentialsException());
        var session = new MockHttpSession();
        String before = session.getId();
        mvc.perform(
                        post("/api/v1/auth/login/").cookie(new Cookie("XSRF-TOKEN", "csrf-fixture")).header("X-XSRF-TOKEN", "csrf-fixture")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"admin@example.invalid\",\"password\":\"WrongSecret42\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(session.getId()).isEqualTo(before);
    }

    @Test
    void signInsAndEmailRequestsAreCountedBeforeAnythingElse() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many sign-in attempts. Please try again later."))
                .when(limiter).consume(eq(RequestLimit.SIGN_IN_EMAIL), anyString());
        mvc.perform(
                        post("/api/v1/auth/login/").cookie(new Cookie("XSRF-TOKEN", "csrf-fixture")).header("X-XSRF-TOKEN", "csrf-fixture")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"admin@example.invalid\",\"password\":\"ValidSecret42\"}"))
                .andExpect(status().isTooManyRequests());
        verify(auth, never()).authenticate(anyString(), anyString());

        mvc.perform(
                        post("/api/v1/auth/users/reset_password/").cookie(new Cookie("XSRF-TOKEN", "csrf-fixture")).header("X-XSRF-TOKEN", "csrf-fixture")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"resident@example.invalid\"}"))
                .andExpect(status().isNoContent());
        verify(limiter).consumeAccountEmail(anyString(), eq("resident@example.invalid"));
    }

    @Test
    void signUpsCountTheirNetworkBeforeAnythingElse() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many sign-ups from this network. Please try again later."))
                .when(limiter).consume(eq(RequestLimit.SIGN_UP_ADDRESS), anyString());
        mvc.perform(
                        post("/api/v1/auth/users/").cookie(new Cookie("XSRF-TOKEN", "csrf-fixture")).header("X-XSRF-TOKEN", "csrf-fixture")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"new_resident\",\"first_name\":\"New\",\"last_name\":\"Resident\","
                                        + "\"email\":\"resident@example.invalid\",\"password\":\"ValidSecret42\","
                                        + "\"re_password\":\"ValidSecret42\"}"))
                .andExpect(status().isTooManyRequests());
        verify(registration, never()).register(any(), anyString());
    }

    @Test
    void aRefreshKeepsItsSignInSoSigningOutRefusesTheOldTokensToo() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        var user = new User();
        user.setId(userId);
        when(jwt.extractUserId("refresh-token", TokenType.REFRESH)).thenReturn(userId);
        when(auth.requireActiveUser(userId)).thenReturn(user);
        when(jwt.isCurrent(eq("refresh-token"), any())).thenReturn(true);
        when(jwt.sessionOf("refresh-token")).thenReturn(Optional.of(session));

        mvc.perform(post("/api/v1/auth/refresh/")
                        .cookie(new Cookie("XSRF-TOKEN", "csrf-fixture"), new Cookie("refresh", "refresh-token"))
                        .header("X-XSRF-TOKEN", "csrf-fixture"))
                .andExpect(status().isOk());
        verify(jwt).issueAccessToken(userId, session);
        verify(jwt).issueRefreshToken(userId, session);
    }

    @Test
    void signingOutRefusesItsTokensFromThenOn() throws Exception {
        mvc.perform(post("/api/v1/auth/logout/")
                        .cookie(new Cookie("XSRF-TOKEN", "csrf-fixture"), new Cookie("access", "access-token"),
                                new Cookie("refresh", "refresh-token"))
                        .header("X-XSRF-TOKEN", "csrf-fixture"))
                .andExpect(status().isNoContent());
        verify(revocations).revoke("access-token");
        verify(revocations).revoke("refresh-token");
    }

    @Test
    void cookieSessionMutationsRequireCsrfEvenAtLogin() throws Exception {
        mvc.perform(post("/api/v1/auth/login/")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.invalid\",\"password\":\"ValidSecret42\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/logout/").cookie(new Cookie("access", "valid-access")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/logout/").cookie(new Cookie("XSRF-TOKEN", "csrf-fixture")).header("X-XSRF-TOKEN", "csrf-fixture")
                .cookie(new Cookie("access", "valid-access")))
                .andExpect(status().isNoContent());
    }

    @Test
    void browserCanBootstrapCsrfWithoutASessionAndUseItsHeader() throws Exception {
        var response = mvc.perform(get("/api/v1/auth/csrf/")).andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", "no-store")).andReturn().getResponse();
        var token = response.getCookie("XSRF-TOKEN");
        assertThat(token).isNotNull();
        mvc.perform(post("/api/v1/auth/login/").cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.invalid\",\"password\":\"ValidSecret42\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/login/").cookie(token).header("X-XSRF-TOKEN", "forged")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.invalid\",\"password\":\"ValidSecret42\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginHintOutlivesAccessAndMatchesRefreshLifetime() throws Exception {
        var response = mvc.perform(post("/api/v1/auth/login/").cookie(new Cookie("XSRF-TOKEN", "csrf-fixture")).header("X-XSRF-TOKEN", "csrf-fixture")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.invalid\",\"password\":\"ValidSecret42\"}"))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertThat(response.getCookie("logged_in").getMaxAge()).isEqualTo(86400);
        assertThat(response.getCookie("logged_in").getMaxAge()).isEqualTo(response.getCookie("refresh").getMaxAge());
        assertThat(response.getCookie("access").getMaxAge()).isEqualTo(1800);
    }

    @Test
    void aBannedAccountsTokensAreRefused() throws Exception {
        var banned = new Profile();
        banned.setReportCount(5);
        when(profiles.findByUserPkid(1L)).thenReturn(Optional.of(banned));

        mvc.perform(get("/api/v1/auth/users/me/").cookie(new Cookie("access", "valid-access")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void privateDetailsRejectAnonymousVisitors() throws Exception {
        mvc.perform(get("/api/v1/posts/example/" )).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/issues/" + UUID.randomUUID() + "/")).andExpect(status().isUnauthorized());
    }
}
