package com.campuscart.common.util;

import java.util.Locale;

/** Normalizes contact identifiers before uniqueness, lookup, or hashing decisions. */
public final class ContactNormalizer {

    private ContactNormalizer() {
    }

    public static String email(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    public static String emailDomain(String value) {
        String normalized = email(value);
        if (normalized == null) {
            return null;
        }
        int at = normalized.lastIndexOf('@');
        return at < 0 || at == normalized.length() - 1 ? null : normalized.substring(at + 1);
    }

    public static String phone(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("[\\s()\\-]", "");
        return normalized.startsWith("+") ? normalized : "+" + normalized;
    }
}
