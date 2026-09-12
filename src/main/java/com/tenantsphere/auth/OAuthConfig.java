package com.tenantsphere.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GoogleOAuthProperties.class)
public class OAuthConfig {

    @Bean
    @ConditionalOnMissingBean
    public GoogleIdentityProvider googleIdentityProvider(GoogleOAuthProperties properties) {
        return new GoogleIdentityClient(RestClient.create(), properties);
    }
}
