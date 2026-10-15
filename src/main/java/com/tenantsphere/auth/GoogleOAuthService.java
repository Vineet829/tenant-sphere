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
import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
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
    private final GoogleIdentityRepository identities;
    private final PendingSignUps pendingSignUps;

    public GoogleOAuthService(
            GoogleOAuthProperties properties,
            OAuthStateService stateService,
            GoogleIdentityProvider identityProvider,
            UserRepository userRepository,
            ProfileRepository profileRepository,
            PasswordEncoder passwordEncoder, GoogleIdentityRepository identities,
            PendingSignUps pendingSignUps) {
        this.properties = properties;
        this.stateService = stateService;
        this.identityProvider = identityProvider;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.identities = identities;
        this.pendingSignUps = pendingSignUps;
    }

    public String authorizationUrl(String provider, String redirectUri, HttpServletRequest request) {
        requireProvider(provider);
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new GoogleAuthenticationException("redirect_uri is required.");
        }
        if (properties.allowedRedirectUris().isEmpty()
                || !properties.allowedRedirectUris().contains(redirectUri)) {
            throw new GoogleAuthenticationException("redirect_uri is not allowed.");
        }

        String state = stateService.issue(redirectUri, request);
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
    public User login(String provider, String state, String code, HttpServletRequest request) {
        requireProvider(provider);
        if (code == null || code.isBlank()) {
            throw new GoogleAuthenticationException("code is required.");
        }
        String redirectUri = stateService
                .consume(state, request)
                .orElseThrow(() -> new GoogleAuthenticationException("Invalid or expired state."));

        GoogleUserInfo info = identityProvider.exchange(code, redirectUri);
        if (!info.emailVerified()) {
            throw new GoogleAuthenticationException(
                    "Google account email is not verified.");
        }
        if (info.sub() == null || info.sub().isBlank() || info.sub().length() > 255
                || info.email() == null || !info.email().contains("@")) {
            throw new GoogleAuthenticationException("Google did not return a valid identity.");
        }
        User user = identities.findById(info.sub()).map(GoogleIdentity::getUser).orElse(null);
        if (user == null) {
            String email = info.email().trim().toLowerCase(Locale.ROOT);
            // Google has verified the email, so a sign-up with it that was never activated gives way.
            pendingSignUps.removeFor(email);
            if (userRepository.existsByEmailIgnoreCase(email)) {
                throw new GoogleAuthenticationException(
                        "An account with this email already exists. Sign in with your password.");
            }
            user = createUser(info, email);
            GoogleIdentity identity = new GoogleIdentity();
            identity.setSubject(info.sub());
            identity.setUser(user);
            identities.save(identity);
        }
        User account = user;
        if (!account.isActive() || profileRepository.findByUserPkid(account.getPkid())
                .map(profile -> profile.isBanned(account)).orElse(false)) {
            throw new GoogleAuthenticationException("This account cannot sign in.");
        }
        user.setLastLogin(Instant.now());
        return user;
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
