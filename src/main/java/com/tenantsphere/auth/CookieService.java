package com.tenantsphere.auth;

import com.tenantsphere.config.AppProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class CookieService {

    private final AppProperties.Cookie config;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public CookieService(AppProperties properties) {
        this.config = properties.cookie();
        this.accessTtl = properties.jwt().accessTokenTtl();
        this.refreshTtl = properties.jwt().refreshTokenTtl();
    }

    public void writeAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        add(response, config.accessName(), accessToken, accessTtl, true);
        add(response, config.refreshName(), refreshToken, refreshTtl, true);
        add(response, config.loggedInName(), "true", accessTtl, false);
    }

    public void clearAuthCookies(HttpServletResponse response) {
        add(response, config.accessName(), "", Duration.ZERO, true);
        add(response, config.refreshName(), "", Duration.ZERO, true);
        add(response, config.loggedInName(), "", Duration.ZERO, false);
    }

    private void add(HttpServletResponse response, String name, String value, Duration maxAge, boolean httpOnly) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path(config.path())
                .secure(config.secure())
                .sameSite(config.sameSite())
                .httpOnly(httpOnly)
                .maxAge(maxAge)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
