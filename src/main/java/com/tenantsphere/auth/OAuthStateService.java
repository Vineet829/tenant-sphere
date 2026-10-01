package com.tenantsphere.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OAuthStateService {
    private static final String ATTRIBUTE = OAuthStateService.class.getName() + ".state";
    private static final Duration TTL = Duration.ofMinutes(10);
    private final SecureRandom random = new SecureRandom();

    public String issue(String redirectUri, HttpServletRequest request) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        HttpSession session = request.getSession(true);
        synchronized (session) {
            session.setAttribute(ATTRIBUTE, new State(value, redirectUri, Instant.now().plus(TTL)));
        }
        return value;
    }

    public Optional<String> consume(String value, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || value == null) return Optional.empty();
        synchronized (session) {
            Object attribute = session.getAttribute(ATTRIBUTE);
            if (!(attribute instanceof State state)) return Optional.empty();
            if (!state.expiresAt().isAfter(Instant.now())) {
                session.removeAttribute(ATTRIBUTE);
                return Optional.empty();
            }
            if (!MessageDigest.isEqual(value.getBytes(StandardCharsets.UTF_8),
                    state.value().getBytes(StandardCharsets.UTF_8))) return Optional.empty();
            // Consume before contacting Google, including when its response fails.
            session.removeAttribute(ATTRIBUTE);
            return Optional.of(state.redirectUri());
        }
    }

    private record State(String value, String redirectUri, Instant expiresAt) implements Serializable {}
}
