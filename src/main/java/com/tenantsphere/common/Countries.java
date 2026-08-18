package com.tenantsphere.common;

import java.util.Locale;

public final class Countries {

    private Countries() {}

    public static String nameOf(String isoCode) {
        if (isoCode == null || isoCode.isBlank()) {
            return "";
        }
        String name = new Locale.Builder()
                .setRegion(isoCode.toUpperCase(Locale.ROOT))
                .build()
                .getDisplayCountry(Locale.ENGLISH);
        return name.equals(isoCode) ? "" : name;
    }
}
