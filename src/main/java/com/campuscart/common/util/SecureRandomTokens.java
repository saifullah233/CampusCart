package com.campuscart.common.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates cryptographically strong, URL-safe opaque tokens.
 *
 * <p>Used for high-entropy secrets that are handed to clients (e.g. refresh tokens).
 * The raw value is shown to the client exactly once; only its hash is persisted (see
 * {@link Hashing#sha256Hex(String)}).</p>
 */
public final class SecureRandomTokens {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    /** Default entropy for opaque tokens: 256 bits. */
    public static final int DEFAULT_TOKEN_BYTES = 32;

    private SecureRandomTokens() {
    }

    /**
     * Returns a URL-safe token carrying {@value #DEFAULT_TOKEN_BYTES} bytes of entropy.
     */
    public static String urlSafeToken() {
        return urlSafeToken(DEFAULT_TOKEN_BYTES);
    }

    /**
     * Returns a URL-safe token carrying {@code numBytes} bytes of entropy.
     *
     * @param numBytes number of random bytes; must be positive
     */
    public static String urlSafeToken(int numBytes) {
        if (numBytes <= 0) {
            throw new IllegalArgumentException("numBytes must be positive");
        }
        byte[] buffer = new byte[numBytes];
        SECURE_RANDOM.nextBytes(buffer);
        return URL_ENCODER.encodeToString(buffer);
    }

    /** Returns a zero-padded numeric code for one-time verification challenges. */
    public static String numericCode(int digits) {
        if (digits < 4 || digits > 8) {
            throw new IllegalArgumentException("digits must be between 4 and 8");
        }
        int bound = (int) Math.pow(10, digits);
        return String.format(java.util.Locale.ROOT, "%0" + digits + "d", SECURE_RANDOM.nextInt(bound));
    }
}
