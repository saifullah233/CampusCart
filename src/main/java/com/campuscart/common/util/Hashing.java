package com.campuscart.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Deterministic one-way hashing for opaque lookup keys.
 *
 * <p>Intended for values that must be located by exact match without storing the
 * original (e.g. refresh tokens): the raw high-entropy token is hashed and only the
 * digest is persisted and indexed. This is <em>not</em> for passwords — passwords use
 * an adaptive, salted encoder ({@code PasswordEncoder}).</p>
 */
public final class Hashing {

    private static final HexFormat HEX = HexFormat.of();

    private Hashing() {
    }

    /**
     * Returns the lower-case hex-encoded SHA-256 digest of {@code value} (64 chars).
     */
    public static String sha256Hex(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JLS on every JVM; this is unreachable.
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
