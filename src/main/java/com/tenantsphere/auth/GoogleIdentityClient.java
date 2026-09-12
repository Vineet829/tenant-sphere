package com.tenantsphere.auth;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

public class GoogleIdentityClient implements GoogleIdentityProvider {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final RestClient restClient;
    private final GoogleOAuthProperties properties;

    public GoogleIdentityClient(RestClient restClient, GoogleOAuthProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public GoogleUserInfo exchange(String code, String redirectUri) {
        String accessToken = requestAccessToken(code, redirectUri);
        return fetchUserInfo(accessToken);
    }

    private String requestAccessToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        Map<String, Object> body;
        try {
            body = restClient
                    .post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(MAP_TYPE);
        } catch (RuntimeException exception) {
            throw new GoogleAuthenticationException("Unable to exchange the authorization code.");
        }

        Object token = body == null ? null : body.get("access_token");
        if (token == null) {
            throw new GoogleAuthenticationException("Google did not return an access token.");
        }
        return token.toString();
    }

    private GoogleUserInfo fetchUserInfo(String accessToken) {
        Map<String, Object> info;
        try {
            info = restClient
                    .get()
                    .uri(USERINFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(MAP_TYPE);
        } catch (RuntimeException exception) {
            throw new GoogleAuthenticationException("Unable to load the Google profile.");
        }

        if (info == null || info.get("email") == null) {
            throw new GoogleAuthenticationException("Google profile did not include an email.");
        }
        return new GoogleUserInfo(
                info.get("email").toString(),
                asString(info.get("given_name")),
                asString(info.get("family_name")),
                asString(info.get("sub")));
    }

    private String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private static final org.springframework.core.ParameterizedTypeReference<Map<String, Object>>
            MAP_TYPE = new org.springframework.core.ParameterizedTypeReference<>() {};
}
