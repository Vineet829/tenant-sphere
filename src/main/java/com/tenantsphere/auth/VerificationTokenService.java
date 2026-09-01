package com.tenantsphere.auth;

import com.tenantsphere.user.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerificationTokenService {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final VerificationTokenRepository tokenRepository;
    private final SecureRandom random = new SecureRandom();

    public VerificationTokenService(VerificationTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public String issue(User user, VerificationPurpose purpose, Duration ttl) {
        tokenRepository.deleteByUserAndPurpose(user.getPkid(), purpose);

        byte[] material = new byte[32];
        random.nextBytes(material);
        String rawToken = ENCODER.encodeToString(material);

        VerificationToken token = new VerificationToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setPurpose(purpose);
        token.setExpiresAt(Instant.now().plus(ttl));
        tokenRepository.save(token);

        return rawToken;
    }

    @Transactional
    public Optional<User> consume(String uid, String rawToken, VerificationPurpose purpose) {
        Optional<UUID> externalId = decodeUid(uid);
        if (externalId.isEmpty()) {
            return Optional.empty();
        }

        Optional<VerificationToken> found =
                tokenRepository.findByTokenHashAndPurpose(hash(rawToken), purpose);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        VerificationToken token = found.get();
        if (!token.isUsable(Instant.now())
                || !token.getUser().getId().equals(externalId.get())) {
            return Optional.empty();
        }

        token.setUsedAt(Instant.now());
        tokenRepository.save(token);
        return Optional.of(token.getUser());
    }

    public String encodeUid(User user) {
        return ENCODER.encodeToString(user.getId().toString().getBytes(StandardCharsets.UTF_8));
    }

    private Optional<UUID> decodeUid(String uid) {
        try {
            String decoded = new String(DECODER.decode(uid), StandardCharsets.UTF_8);
            return Optional.of(UUID.fromString(decoded));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return ENCODER.encodeToString(
                    digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
