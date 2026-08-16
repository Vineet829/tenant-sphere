package com.tenantsphere.issue;

import com.fasterxml.jackson.annotation.JsonValue;

public enum IssueStatus {
    REPORTED("reported"),
    RESOLVED("resolved"),
    IN_PROGRESS("in_progress");

    private final String value;

    IssueStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static IssueStatus fromValue(String value) {
        for (IssueStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
