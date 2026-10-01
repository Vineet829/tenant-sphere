package com.tenantsphere.common;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class Countries {

    private static final Set<String> ISO_CODES = Set.of(Locale.getISOCountries());
    private static final Map<String, String> NAME_TO_CODE = buildNameIndex();

    private Countries() {}

    public static String nameOf(String isoCode) {
        if (isoCode == null || isoCode.isBlank()) {
            return "";
        }
        String code = isoCode.trim().toUpperCase(Locale.ROOT);
        if (!ISO_CODES.contains(code)) return "";
        return new Locale.Builder()
                .setRegion(code)
                .build()
                .getDisplayCountry(Locale.ENGLISH);
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
