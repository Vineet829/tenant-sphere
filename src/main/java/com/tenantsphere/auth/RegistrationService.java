package com.tenantsphere.auth;

import com.tenantsphere.auth.dto.RegisterRequest;
import com.tenantsphere.common.Slugs;
import com.tenantsphere.config.AppProperties;
import com.tenantsphere.profile.Profile;
import com.tenantsphere.profile.ProfileRepository;
import com.tenantsphere.user.User;
import com.tenantsphere.user.UserRepository;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenService tokenService;
    private final VerificationMailer mailer;
    private final Duration activationTtl;

    public RegistrationService(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            PasswordEncoder passwordEncoder,
            VerificationTokenService tokenService,
            VerificationMailer mailer,
            AppProperties properties) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mailer = mailer;
        this.activationTtl = properties.verification().activationTtl();
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (!request.password().equals(request.rePassword())) {
            throw new RegistrationException("re_password", "Passwords do not match");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new RegistrationException("email", "A user with this email already exists");
        }
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new RegistrationException("username", "A user with this username already exists");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setActive(false);
        userRepository.save(user);

        Profile profile = new Profile();
        profile.setUser(user);
        profile.setSlug(uniqueSlug(request.username()));
        profileRepository.save(profile);

        sendActivation(user);
        return user;
    }

    @Transactional
    public void resendActivation(String email) {
        Optional<User> user = userRepository.findByEmailIgnoreCase(email.trim());
        user.filter(candidate -> !candidate.isActive()).ifPresent(this::sendActivation);
    }

    @Transactional
    public boolean activate(String uid, String token) {
        Optional<User> user = tokenService.consume(uid, token, VerificationPurpose.ACTIVATION);
        if (user.isEmpty()) {
            return false;
        }
        User activated = user.get();
        activated.setActive(true);
        userRepository.save(activated);
        return true;
    }

    private void sendActivation(User user) {
        String token = tokenService.issue(user, VerificationPurpose.ACTIVATION, activationTtl);
        mailer.sendActivation(user, tokenService.encodeUid(user), token);
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
}
