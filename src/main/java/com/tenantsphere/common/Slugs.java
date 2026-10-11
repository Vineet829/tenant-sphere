package com.tenantsphere.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Slugs {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("(^-|-$)");

    private Slugs() {}

    /**
     * The value's slug, or the fallback when nothing of it is left, as with a title written wholly in
     * another script, such as Hindi: an empty slug makes an address nothing can be found at.
     */
    public static String of(String value, String fallback) {
        String slug = of(value);
        return slug.isEmpty() ? fallback : slug;
    }

    public static String of(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return EDGE_HYPHENS.matcher(NON_ALPHANUMERIC.matcher(normalized).replaceAll("-"))
                .replaceAll("");
    }
}
