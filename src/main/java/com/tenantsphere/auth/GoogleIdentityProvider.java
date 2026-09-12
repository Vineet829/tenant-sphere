package com.tenantsphere.auth;

public interface GoogleIdentityProvider {

    GoogleUserInfo exchange(String code, String redirectUri);
}
