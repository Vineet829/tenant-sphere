package com.tenantsphere.auth;

import com.fasterxml.jackson.annotation.JsonValue;

public enum VerificationPurpose {
    ACTIVATION("activation"),
    PASSWORD_RESET("password_reset");

    private final String value;

    VerificationPurpose(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static VerificationPurpose fromValue(String value) {
        for (VerificationPurpose purpose : values()) {
            if (purpose.value.equals(value)) {
                return purpose;
            }
        }
        throw new IllegalArgumentException("Unknown purpose: " + value);
    }
}
