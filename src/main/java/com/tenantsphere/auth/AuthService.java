package com.tenantsphere.auth;

import com.tenantsphere.profile.Profile;
import com.tenantsphere.profile.ProfileRepository;
import com.tenantsphere.user.User;
import com.tenantsphere.user.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String DUMMY_HASH =
            "$argon2id$v=19$m=16384,t=2,p=1$AAAAAAAAAAAAAAAA$AAAAAAAAAAAAAAAAAAAAAA";

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (user == null) {
            passwordEncoder.matches(rawPassword, DUMMY_HASH);
            throw new InvalidCredentialsException();
        }

        String encodedPassword = user.getPassword();
        boolean unusablePassword = encodedPassword == null || encodedPassword.startsWith("!");
        boolean passwordMatches = passwordEncoder.matches(rawPassword,
                user.isGuest() || unusablePassword ? DUMMY_HASH : encodedPassword);
        if (!passwordMatches || unusablePassword || user.isGuest() || !canSignIn(user)) {
            throw new InvalidCredentialsException();
        }

        user.setLastLogin(Instant.now());
        return user;
    }

    @Transactional(readOnly = true)
    public User requireActiveUser(UUID externalId) {
        User user = userRepository.findByExternalId(externalId)
                .orElseThrow(InvalidCredentialsException::new);
        if (!canSignIn(user)) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    private boolean canSignIn(User user) {
        return user.isSessionEligible() && !profileRepository.findByUserPkid(user.getPkid())
                .map(Profile::isBanned).orElse(false);
    }

    @Transactional(readOnly = true)
    public Profile requireProfile(User user) {
        return profileRepository.findByUserPkid(user.getPkid())
                .orElseThrow(() -> new IllegalStateException(
                        "No profile for user " + user.getEmail()));
    }
}
