package com.JobsNow.backend.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtil {

    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9-]+");

    private SlugUtil() {
    }

    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "post";
        }
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);
        String noMarks = normalized.replaceAll("\\p{M}+", "");
        String lower = noMarks.toLowerCase(Locale.ROOT);
        String slug = NON_SLUG.matcher(lower).replaceAll("-");
        slug = slug.replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
        return slug.isEmpty() ? "post" : slug;
    }
}
