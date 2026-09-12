package com.tenantsphere.auth;

import com.tenantsphere.common.Slugs;
import com.tenantsphere.profile.Profile;
import com.tenantsphere.profile.ProfileRepository;
import com.tenantsphere.user.User;
import com.tenantsphere.user.UserRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleOAuthService {

    private static final String PROVIDER = "google-oauth2";
    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String SCOPE = "openid email profile";

    private final GoogleOAuthProperties properties;
    private final OAuthStateService stateService;
    private final GoogleIdentityProvider identityProvider;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    public GoogleOAuthService(
            GoogleOAuthProperties properties,
            OAuthStateService stateService,
            GoogleIdentityProvider identityProvider,
            UserRepository userRepository,
            ProfileRepository profileRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.stateService = stateService;
        this.identityProvider = identityProvider;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String authorizationUrl(String provider, String redirectUri) {
        requireProvider(provider);
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new GoogleAuthenticationException("redirect_uri is required.");
        }
        if (!properties.allowedRedirectUris().isEmpty()
                && !properties.allowedRedirectUris().contains(redirectUri)) {
            throw new GoogleAuthenticationException("redirect_uri is not allowed.");
        }

        String state = stateService.issue(redirectUri);
        return AUTH_ENDPOINT
                + "?response_type=code"
                + "&client_id=" + encode(properties.clientId())
                + "&redirect_uri=" + encode(redirectUri)
                + "&scope=" + encode(SCOPE)
                + "&state=" + encode(state)
                + "&access_type=offline"
                + "&prompt=select_account";
    }

    @Transactional
    public User login(String provider, String state, String code) {
        requireProvider(provider);
        if (code == null || code.isBlank()) {
            throw new GoogleAuthenticationException("code is required.");
        }
        String redirectUri = stateService
                .verify(state)
                .orElseThrow(() -> new GoogleAuthenticationException("Invalid or expired state."));

        GoogleUserInfo info = identityProvider.exchange(code, redirectUri);
        String email = info.email().trim().toLowerCase(Locale.ROOT);

        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> createUser(info, email));
    }

    private User createUser(GoogleUserInfo info, String email) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(uniqueUsername(info, email));
        user.setFirstName(defaultText(info.firstName(), "Google"));
        user.setLastName(defaultText(info.lastName(), "User"));
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setActive(true);
        userRepository.save(user);

        Profile profile = new Profile();
        profile.setUser(user);
        profile.setSlug(uniqueSlug(user.getUsername()));
        profileRepository.save(profile);
        return user;
    }

    private void requireProvider(String provider) {
        if (!PROVIDER.equals(provider)) {
            throw new GoogleAuthenticationException("Provider \"" + provider + "\" is not supported.");
        }
    }

    private String uniqueUsername(GoogleUserInfo info, String email) {
        String base = Slugs.of(info.firstName() + " " + info.lastName());
        if (base.isBlank()) {
            base = Slugs.of(email.substring(0, email.indexOf('@')));
        }
        if (base.isBlank()) {
            base = "user";
        }
        String candidate = base;
        while (userRepository.existsByUsernameIgnoreCase(candidate)) {
            candidate = base + "-" + Integer.toString((int) (Math.random() * 100000), 36);
        }
        return candidate;
    }

    private String uniqueSlug(String username) {
        String base = Slugs.of(username);
        String candidate = base;
        int suffix = 1;
        while (profileRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
