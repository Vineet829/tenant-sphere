package com.tenantsphere.config;

import com.tenantsphere.auth.JwtCookieAuthenticationFilter;
import com.tenantsphere.auth.TokenRevocations;
import com.tenantsphere.admin.AdminLoginView;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.spring.security.NavigationAccessControlConfigurer;
import com.vaadin.flow.spring.security.RequestUtil;
import com.tenantsphere.common.ApiAccessDeniedHandler;
import com.tenantsphere.common.ApiAuthenticationEntryPoint;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(AppProperties.class)
public class SecurityConfig {

    private final JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;
    private final ApiAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter,
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler) {
        this.jwtCookieAuthenticationFilter = jwtCookieAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BoundedPasswordEncoder(Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
    }

    @Bean
    public AuthenticationContext authenticationContext() {
        return new AuthenticationContext();
    }

    @Bean
    public static NavigationAccessControlConfigurer navigationAccessControlConfigurer() {
        return new NavigationAccessControlConfigurer()
                .withAnnotatedViewAccessChecker()
                .withLoginView(AdminLoginView.class);
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http, AuthenticationContext authenticationContext, AppProperties properties,
            RequestUtil requestUtil, TokenRevocations revocations) throws Exception {
        var csrfRepository = new org.springframework.security.web.csrf.CookieCsrfTokenRepository();
        csrfRepository.setCookieName(properties.cookie().secure() ? "__Host-XSRF-TOKEN" : "XSRF-TOKEN");
        csrfRepository.setCookieCustomizer(cookie -> cookie.path("/").httpOnly(false)
                .secure(properties.cookie().secure()).sameSite("Lax"));
        http
            .csrf(csrf -> csrf
                    .csrfTokenRepository(csrfRepository)
                    // The token is read from a cookie, never reflected in a compressed response body.
                    .csrfTokenRequestHandler(new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler())
                    .ignoringRequestMatchers(request -> !request.getRequestURI().startsWith("/api/")
                            && !request.getRequestURI().equals("/admin/logout")))
            // JWT authentication is checked on every request. Applying the session-fixation
            // strategy on each check rotates Vaadin's UI session and races concurrent uploads.
            // Both password-login entry points rotate an existing session explicitly instead.
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .sessionFixation(f -> f.none()))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR)
                .permitAll()
                // Vaadin initializes its login UI through the servlet root. View annotations
                // enforce navigation access; this matcher never applies to REST endpoints.
                .requestMatchers(request -> request.getRequestURI().equals(request.getContextPath() + "/admin/")
                        && requestUtil.isFrameworkInternalRequest(request))
                .permitAll()
                .requestMatchers(
                    "/actuator/health",
                    "/api/v1/auth/login/",
                    "/api/v1/auth/csrf/",
                    "/api/v1/auth/guest/",
                    "/api/v1/auth/refresh/",
                    "/api/v1/auth/logout/",
                    "/api/v1/auth/users/",
                    "/api/v1/auth/users/activation/",
                    "/api/v1/auth/users/resend_activation/",
                    "/api/v1/auth/users/reset_password/",
                    "/api/v1/auth/users/reset_password_confirm/",
                    "/api/v1/auth/o/*/",
                    "/admin/login",
                    "/admin/login/",
                    "/admin/VAADIN/**",
                    "/admin-assets/**",
                    "/supersecret",
                    "/supersecret/**",
                    "/VAADIN/**",
                    "/media/**")
                .permitAll()
                .requestMatchers("/admin", "/admin/**").hasAnyRole("STAFF", "SUPERUSER")
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/posts/",
                    "/api/v1/posts/top-posts/",
                    "/api/v1/posts/popular-tags/",
                    "/api/v1/posts/tags/**")
                .permitAll()
                .anyRequest().authenticated()
            )
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                // The console's tokens are refused from now on, not only taken from this browser.
                .addLogoutHandler((request, response, authentication) -> {
                    if (request.getCookies() == null) return;
                    for (var cookie : request.getCookies()) {
                        if (cookie.getName().equals(properties.cookie().accessName())
                                || cookie.getName().equals(properties.cookie().refreshName())) {
                            revocations.revoke(cookie.getValue());
                        }
                    }
                })
                .logoutSuccessUrl("/admin/login")
                .deleteCookies(properties.cookie().accessName(), properties.cookie().refreshName(),
                        properties.cookie().loggedInName()))
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint((request, response, error) -> {
                    if (request.getRequestURI().startsWith("/admin") && "GET".equals(request.getMethod())) {
                        response.sendRedirect("/admin/login");
                    } else authenticationEntryPoint.commence(request, response, error);
                })
                .accessDeniedHandler(accessDeniedHandler)
            )
            .addFilterBefore(jwtCookieAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        SecurityFilterChain chain = http.build();
        AuthenticationContext.applySecurityConfiguration(http, authenticationContext);
        return chain;
    }
}
