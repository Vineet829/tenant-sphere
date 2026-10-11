package com.tenantsphere.config;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

/**
 * Hashes at most a couple of passwords at once. Each Argon2 hash holds 16 MiB of memory while it
 * runs, so on a small server a burst of sign-ins could otherwise take more memory than it has. A hash
 * waits its turn for a few seconds at most; past that the server is too busy, and the request is
 * refused, to be tried again, rather than left waiting.
 */
public class BoundedPasswordEncoder implements PasswordEncoder {
    static final int AT_ONCE = 2;
    static final Duration WAIT = Duration.ofSeconds(10);

    private final PasswordEncoder delegate;
    private final Semaphore turns;
    private final Duration wait;

    public BoundedPasswordEncoder(PasswordEncoder delegate) {
        this(delegate, AT_ONCE, WAIT);
    }

    BoundedPasswordEncoder(PasswordEncoder delegate, int atOnce, Duration wait) {
        this.delegate = delegate;
        this.turns = new Semaphore(atOnce, true);
        this.wait = wait;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return inTurn(() -> delegate.encode(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return inTurn(() -> delegate.matches(rawPassword, encodedPassword));
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return delegate.upgradeEncoding(encodedPassword);
    }

    private <T> T inTurn(Supplier<T> hash) {
        try {
            if (!turns.tryAcquire(wait.toMillis(), TimeUnit.MILLISECONDS)) throw busy();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw busy();
        }
        try {
            return hash.get();
        } finally {
            turns.release();
        }
    }

    private static ResponseStatusException busy() {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "The server is busy. Please try again in a moment.");
    }
}
