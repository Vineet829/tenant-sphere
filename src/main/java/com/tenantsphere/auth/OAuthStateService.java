package com.tenantsphere.auth;

import com.tenantsphere.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class OAuthStateService {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final Duration TTL = Duration.ofMinutes(10);

    private final byte[] key;

    public OAuthStateService(AppProperties properties) {
        this.key = properties.jwt().signingKey().getBytes(StandardCharsets.UTF_8);
    }

    public String issue(String redirectUri) {
        String payload = "%s|%d|%s".formatted(
                redirectUri, Instant.now().plus(TTL).toEpochMilli(), UUID.randomUUID());
        String encoded = ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encoded + "." + sign(encoded);
    }

    public Optional<String> verify(String state) {
        if (state == null) {
            return Optional.empty();
        }
        int dot = state.indexOf('.');
        if (dot < 0) {
            return Optional.empty();
        }
        String encoded = state.substring(0, dot);
        String signature = state.substring(dot + 1);
        if (!constantTimeEquals(signature, sign(encoded))) {
            return Optional.empty();
        }

        String payload;
        try {
            payload = new String(DECODER.decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }

        String[] parts = payload.split("\\|", 3);
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            if (Instant.now().toEpochMilli() > Long.parseLong(parts[1])) {
                return Optional.empty();
            }
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
        return Optional.of(parts[0]);
    }

    private String sign(String encoded) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return ENCODER.encodeToString(mac.doFinal(encoded.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
