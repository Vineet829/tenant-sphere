package com.tenantsphere.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

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
    @MockBean RegistrationService registration;
    @MockBean PasswordResetService passwordReset;
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
        when(jwt.issueAccessToken(any())).thenReturn("valid-access");
        when(jwt.issueRefreshToken(any())).thenReturn("valid-refresh");
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
    void privateDetailsRejectAnonymousVisitors() throws Exception {
        mvc.perform(get("/api/v1/posts/example/" )).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/issues/" + UUID.randomUUID() + "/")).andExpect(status().isUnauthorized());
    }
}
