package com.campuscart.common.exception;

/**
 * Thrown when a presented token (refresh token, or a token parsed outside the security
 * filter chain) is missing, malformed, expired, revoked, or replayed. Maps to HTTP 401.
 *
 * <p>The client-facing message is deliberately generic and identical across every cause:
 * revealing whether a token was expired versus forged versus reused only helps an
 * attacker. The specific reason is logged server-side, never returned.</p>
 */
public class InvalidTokenException extends ApiException {

    private static final String GENERIC_MESSAGE = "The token is invalid or has expired.";

    public InvalidTokenException() {
        super(ErrorCode.INVALID_TOKEN, GENERIC_MESSAGE);
    }

    public InvalidTokenException(Throwable cause) {
        super(ErrorCode.INVALID_TOKEN, GENERIC_MESSAGE, cause);
    }
}
