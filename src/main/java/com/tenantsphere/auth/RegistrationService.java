package com.tenantsphere.auth;

import com.tenantsphere.auth.dto.RegisterRequest;
import com.tenantsphere.common.Slugs;
import com.tenantsphere.common.AfterCommit;
import com.tenantsphere.config.AppProperties;
import com.tenantsphere.profile.Profile;
import com.tenantsphere.profile.ProfileRepository;
import com.tenantsphere.user.User;
import com.tenantsphere.user.UserRepository;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
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
    private final PendingSignUps pendingSignUps;
    private final RequestLimiter limiter;
    private final Duration activationTtl;

    public RegistrationService(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            PasswordEncoder passwordEncoder,
            VerificationTokenService tokenService,
            VerificationMailer mailer,
            PendingSignUps pendingSignUps,
            RequestLimiter limiter,
            AppProperties properties) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mailer = mailer;
        this.pendingSignUps = pendingSignUps;
        this.limiter = limiter;
        this.activationTtl = properties.verification().activationTtl();
    }

    @Transactional
    public User register(RegisterRequest request, String clientAddress) {
        if (!request.password().equals(request.rePassword())) {
            throw new RegistrationException("re_password", "Passwords do not match");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        // A sign-up never activated gives way to this one: only its newest link works.
        pendingSignUps.removeFor(email);
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

        // From here on the sign-up emails its address: an activation link, or a note to the account it has.
        // It counts toward the account emails alike in both cases, once its password is hashed, so their
        // counts are held only until it is saved. One refused before, as for a taken username, sends nothing
        // and isn't counted.
        limiter.consumeAccountEmail(clientAddress, email);

        // An email that has an account gets a new sign-up's answer, so signing up doesn't tell
        // anyone which emails have accounts here. The account's owner is told instead.
        Optional<User> owner = userRepository.findByEmailIgnoreCase(email);
        if (owner.isPresent()) {
            user.setId(UUID.randomUUID());
            if (!owner.get().isGuest()) {
                AfterCommit.run(() -> mailer.sendSignUpAttempt(owner.get()));
            }
            return user;
        }
        userRepository.save(user);

        Profile profile = new Profile();
        profile.setUser(user);
        profile.setSlug(uniqueSlug(request.username()));
        profileRepository.save(profile);

        sendActivation(user);
        return user;
    }

    // An activation email is only for an account that was never turned on.
    // One that staff or reports turned off stays off until staff turn it on.
    @Transactional
    public void resendActivation(String email) {
        Optional<User> user = userRepository.findByEmailIgnoreCase(email.trim());
        user.filter(candidate -> awaitingActivation(candidate) && !isBanned(candidate))
                .ifPresent(this::sendActivation);
    }

    @Transactional
    public boolean activate(String uid, String token) {
        Optional<User> user = tokenService.consume(uid, token, VerificationPurpose.ACTIVATION);
        if (user.isEmpty()) {
            return false;
        }
        User activated = user.get();
        if (!awaitingActivation(activated) || isBanned(activated)) {
            return false;
        }
        activated.setActive(true);
        userRepository.save(activated);
        return true;
    }

    private static boolean awaitingActivation(User user) {
        return !user.isActive() && user.getActivatedAt() == null;
    }

    private void sendActivation(User user) {
        String token = tokenService.issue(user, VerificationPurpose.ACTIVATION, activationTtl);
        AfterCommit.run(() -> mailer.sendActivation(user, tokenService.encodeUid(user), token));
    }

    private boolean isBanned(User user) {
        return profileRepository.findByUserPkid(user.getPkid()).map(Profile::isBanned).orElse(false);
    }

    // A username made only of underscores has no letters for a slug, so its profile takes a plain one.
    private String uniqueSlug(String username) {
        String base = Slugs.of(username, "profile");
        String candidate = base;
        int suffix = 1;
        while (profileRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
