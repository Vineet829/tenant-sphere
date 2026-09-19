package com.tenantsphere.auth;

import com.tenantsphere.config.AppProperties;
import com.tenantsphere.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class VerificationMailer {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerificationMailer.class);

    private final JavaMailSender mailSender;
    private final AppProperties.Site site;

    public VerificationMailer(JavaMailSender mailSender, AppProperties properties) {
        this.mailSender = mailSender;
        this.site = properties.site();
    }

    @Async
    public void sendActivation(User user, String uid, String token) {
        String link = "%s/activate/%s/%s".formatted(site.domain(), uid, token);
        send(
                user,
                "Activate your %s account".formatted(site.name()),
                """
                Hello %s,

                Thank you for signing up to %s. Please activate your account by \
                opening the link below.

                %s

                If you did not create this account you can ignore this message.
                """
                        .formatted(user.getFirstName(), site.name(), link));
    }

    @Async
    public void sendPasswordReset(User user, String uid, String token) {
        String link = "%s/password-reset/%s/%s".formatted(site.domain(), uid, token);
        send(
                user,
                "Reset your %s password".formatted(site.name()),
                """
                Hello %s,

                We received a request to reset your %s password. Open the link below \
                to choose a new one.

                %s

                If you did not request this you can ignore this message.
                """
                        .formatted(user.getFirstName(), site.name(), link));
    }

    @Async
    public void sendPasswordChanged(User user) {
        send(
                user,
                "Your %s password was changed".formatted(site.name()),
                """
                Hello %s,

                This is a confirmation that the password for your %s account was \
                just changed.

                If you did not make this change please contact support immediately.
                """
                        .formatted(user.getFirstName(), site.name()));
    }

    private void send(User user, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(site.defaultFromEmail());
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            LOGGER.error("Failed to send \"{}\" to {}", subject, user.getEmail(), exception);
        }
    }
}
