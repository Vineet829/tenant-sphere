package com.tenantsphere.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cookie cookie, Site site) {

    public record Jwt(String signingKey, Duration accessTokenTtl, Duration refreshTokenTtl) {}

    public record Cookie(
            String accessName,
            String refreshName,
            String loggedInName,
            String path,
            String sameSite,
            boolean secure) {}

    public record Site(String name, String domain, String defaultFromEmail) {}
}
