package com.tenantsphere.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tenantsphere.config.AppProperties;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private static final UUID SIGN_IN = UUID.randomUUID();

    @Test
    void credentialChangesRevokeBothTokenTypes() {
        JwtService jwt = service("a", Duration.ofMinutes(30));
        var user = new com.tenantsphere.user.User();
        user.setId(UUID.randomUUID());
        String access = jwt.issueAccessToken(user.getId(), SIGN_IN);
        String refresh = jwt.issueRefreshToken(user.getId(), SIGN_IN);
        assertThat(jwt.isCurrent(access, user)).isTrue();
        user.setAuthVersion(1);
        assertThat(jwt.isCurrent(access, user)).isFalse();
        assertThat(jwt.isCurrent(refresh, user)).isFalse();
    }

    @Test
    void accessAndRefreshTokensRoundTripTheirUserAndHaveDifferentIds() {
        JwtService jwt = service("a", Duration.ofMinutes(30));
        UUID user = UUID.randomUUID();
        String access = jwt.issueAccessToken(user, SIGN_IN);
        assertThat(jwt.extractUserId(access, TokenType.ACCESS)).isEqualTo(user);
        assertThat(jwt.extractUserId(jwt.issueRefreshToken(user, SIGN_IN), TokenType.REFRESH)).isEqualTo(user);
        assertThat(jwt.issueAccessToken(user, SIGN_IN)).isNotEqualTo(access);
    }

    @Test
    void tokenTypesCannotBeInterchanged() {
        JwtService jwt = service("a", Duration.ofMinutes(30));
        UUID user = UUID.randomUUID();
        assertThatThrownBy(() -> jwt.extractUserId(jwt.issueRefreshToken(user, SIGN_IN), TokenType.ACCESS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> jwt.extractUserId(jwt.issueAccessToken(user, SIGN_IN), TokenType.REFRESH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expiredAccessAndRefreshTokensAreRejected() {
        JwtService jwt = service("a", Duration.ofSeconds(-1));
        UUID user = UUID.randomUUID();
        assertThatThrownBy(() -> jwt.extractUserId(jwt.issueAccessToken(user, SIGN_IN), TokenType.ACCESS))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> jwt.extractUserId(jwt.issueRefreshToken(user, SIGN_IN), TokenType.REFRESH))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void signatureFromAnotherKeyIsRejected() {
        String token = service("b", Duration.ofMinutes(30)).issueAccessToken(UUID.randomUUID(), SIGN_IN);
        assertThatThrownBy(() -> service("a", Duration.ofMinutes(30)).extractUserId(token, TokenType.ACCESS))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void aSignInsTokensCarryItsSessionId() {
        JwtService jwt = service("a", Duration.ofMinutes(30));
        UUID user = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        assertThat(jwt.sessionOf(jwt.issueAccessToken(user, session))).contains(session);
        assertThat(jwt.sessionOf(jwt.issueRefreshToken(user, session))).contains(session);
        assertThat(jwt.sessionOf("not-a-token")).isEmpty();
        assertThat(jwt.sessionOf(service("b", Duration.ofMinutes(30)).issueAccessToken(user, session))).isEmpty();
    }

    private JwtService service(String key, Duration ttl) {
        return new JwtService(new AppProperties(new AppProperties.Jwt(key.repeat(64), ttl, ttl), null, null, null));
    }
}
