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
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive() || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        user.setLastLogin(Instant.now());
        return user;
    }

    @Transactional(readOnly = true)
    public User requireActiveUser(UUID externalId) {
        User user = userRepository.findByExternalId(externalId)
                .orElseThrow(InvalidCredentialsException::new);
        if (!user.isActive()) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    @Transactional(readOnly = true)
    public Profile requireProfile(User user) {
        return profileRepository.findByUserPkid(user.getPkid())
                .orElseThrow(() -> new IllegalStateException(
                        "No profile for user " + user.getEmail()));
    }
}
