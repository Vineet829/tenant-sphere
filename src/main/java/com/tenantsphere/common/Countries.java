package com.tenantsphere.common;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class Countries {

    private static final Map<String, String> NAME_TO_CODE = buildNameIndex();

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

    public static Optional<String> codeOf(String country) {
        if (country == null || country.isBlank()) {
            return Optional.empty();
        }
        String trimmed = country.trim();
        if (trimmed.length() == 2 && !nameOf(trimmed).isEmpty()) {
            return Optional.of(trimmed.toUpperCase(Locale.ROOT));
        }
        return Optional.ofNullable(NAME_TO_CODE.get(trimmed.toLowerCase(Locale.ROOT)));
    }

    private static Map<String, String> buildNameIndex() {
        Map<String, String> index = new HashMap<>();
        for (String code : Locale.getISOCountries()) {
            String name = nameOf(code);
            if (!name.isEmpty()) {
                index.put(name.toLowerCase(Locale.ROOT), code);
            }
        }
        return index;
    }
}
