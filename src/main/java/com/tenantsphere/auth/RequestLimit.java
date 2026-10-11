package com.tenantsphere.auth;

import java.time.Duration;

/** How many requests of each kind are taken in a window, and what is said once there are too many. */
public enum RequestLimit {
    SIGN_IN_ADDRESS(30, Duration.ofMinutes(15), "Too many sign-in attempts. Please try again later."),
    SIGN_IN_EMAIL(10, Duration.ofMinutes(15), "Too many sign-in attempts. Please try again later."),
    SIGN_UP_ADDRESS(5, Duration.ofHours(1), "Too many sign-ups from this network. Please try again later."),
    // Activation and password reset emails: from one network, to one address, and in all, so the
    // mailbox they are sent from stays within its provider's sending limits.
    ACCOUNT_EMAIL_ADDRESS(5, Duration.ofHours(1), "Too many emails requested. Please try again later."),
    ACCOUNT_EMAIL_RECIPIENT(3, Duration.ofHours(1), "Too many emails requested. Please try again later."),
    ACCOUNT_EMAIL_ALL(100, Duration.ofHours(1), "Too many emails requested. Please try again later."),
    GOOGLE_SIGN_IN_ADDRESS(20, Duration.ofMinutes(15), "Too many sign-in attempts. Please try again later.");

    final int most;
    final Duration window;
    final String refusal;

    RequestLimit(int most, Duration window, String refusal) {
        this.most = most;
        this.window = window;
        this.refusal = refusal;
    }
}
