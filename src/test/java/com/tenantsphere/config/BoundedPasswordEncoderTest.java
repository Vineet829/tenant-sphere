package com.tenantsphere.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class BoundedPasswordEncoderTest {

    @Test
    void hashesAtMostACoupleAtOnceAndRefusesOnceTheWaitRunsOut() throws Exception {
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        PasswordEncoder slow = new PasswordEncoder() {
            @Override
            public String encode(CharSequence raw) {
                started.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "hash";
            }

            @Override
            public boolean matches(CharSequence raw, String encoded) {
                return true;
            }
        };
        BoundedPasswordEncoder bounded = new BoundedPasswordEncoder(slow, 2, Duration.ofMillis(200));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = pool.submit(() -> bounded.encode("first"));
            Future<String> second = pool.submit(() -> bounded.encode("second"));
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> bounded.matches("third", "hash"))
                    .isInstanceOfSatisfying(ResponseStatusException.class, busy -> {
                        assertThat(busy.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                        assertThat(busy.getReason()).isEqualTo("The server is busy. Please try again in a moment.");
                    });

            release.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo("hash");
            assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo("hash");
            assertThat(bounded.matches("third", "hash")).isTrue();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void theAppHashesTwoPasswordsAtOnceAndWaitsTenSecondsAtMost() {
        assertThat(BoundedPasswordEncoder.AT_ONCE).isEqualTo(2);
        assertThat(BoundedPasswordEncoder.WAIT).isEqualTo(Duration.ofSeconds(10));
    }
}
