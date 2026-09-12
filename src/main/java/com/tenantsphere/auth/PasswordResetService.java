package com.tenantsphere.auth;

import com.tenantsphere.config.AppProperties;
import com.tenantsphere.user.User;
import com.tenantsphere.user.UserRepository;
import java.time.Duration;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenService tokenService;
    private final VerificationMailer mailer;
    private final Duration passwordResetTtl;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            VerificationTokenService tokenService,
            VerificationMailer mailer,
            AppProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mailer = mailer;
        this.passwordResetTtl = properties.verification().passwordResetTtl();
    }

    @Transactional
    public void requestReset(String email) {
        Optional<User> user = userRepository.findByEmailIgnoreCase(email.trim());
        user.filter(User::isActive).ifPresent(candidate -> {
            String token =
                    tokenService.issue(candidate, VerificationPurpose.PASSWORD_RESET, passwordResetTtl);
            mailer.sendPasswordReset(candidate, tokenService.encodeUid(candidate), token);
        });
    }

    @Transactional
    public void confirmReset(String uid, String token, String newPassword, String reNewPassword) {
        if (!newPassword.equals(reNewPassword)) {
            throw new RegistrationException(
                    "re_new_password", "The two password fields didn't match.");
        }

        User user = tokenService
                .consume(uid, token, VerificationPurpose.PASSWORD_RESET)
                .orElseThrow(InvalidPasswordResetTokenException::new);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        mailer.sendPasswordChanged(user);
    }
}
