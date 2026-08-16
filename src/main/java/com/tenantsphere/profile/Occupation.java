package com.tenantsphere.profile;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Occupation {
    MASON("mason"),
    CARPENTER("carpenter"),
    PLUMBER("plumber"),
    ROOFER("roofer"),
    PAINTER("painter"),
    ELECTRICIAN("electrician"),
    HVAC("hvac"),
    TENANT("tenant");

    private final String value;

    Occupation(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static Occupation fromValue(String value) {
        for (Occupation occupation : values()) {
            if (occupation.value.equals(value)) {
                return occupation;
            }
        }
        throw new IllegalArgumentException("Unknown occupation: " + value);
    }
}
