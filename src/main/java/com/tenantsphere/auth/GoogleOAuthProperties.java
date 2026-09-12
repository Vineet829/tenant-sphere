package com.tenantsphere.auth;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth.google")
public record GoogleOAuthProperties(
        String clientId, String clientSecret, List<String> allowedRedirectUris) {

    public GoogleOAuthProperties {
        allowedRedirectUris = allowedRedirectUris == null ? List.of() : List.copyOf(allowedRedirectUris);
    }
}
