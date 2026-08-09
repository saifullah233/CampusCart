package com.campuscart.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Stable, machine-readable error codes surfaced to API clients.
 *
 * <p>Each code carries its canonical HTTP status so handlers never hard-code
 * numeric statuses (no magic strings/numbers).</p>
 */
public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST),
    CONSTRAINT_VIOLATION(HttpStatus.CONFLICT),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    BUSINESS_RULE_VIOLATION(HttpStatus.CONFLICT),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),

    // --- Authentication / authorization (Part 3 security foundation) ---
    // These are intentionally coarse-grained: the response never reveals *why* auth
    // failed (missing vs expired vs malformed token, unknown-user vs wrong-password),
    // since that distinction only aids an attacker.
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(HttpStatus.FORBIDDEN),
    ACCOUNT_NOT_ACTIVE(HttpStatus.FORBIDDEN),
    OTP_INVALID(HttpStatus.BAD_REQUEST),
    OTP_ALREADY_VERIFIED(HttpStatus.CONFLICT),
    OTP_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS),
    OTP_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS),
    OTP_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),

    INVALID_IMAGE(HttpStatus.BAD_REQUEST),
    IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST),
    MEDIA_STORAGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    PRODUCT_UNAVAILABLE(HttpStatus.CONFLICT),
    CART_EMPTY(HttpStatus.CONFLICT),
    ORDER_STATE_INVALID(HttpStatus.CONFLICT),
    PAYMENT_INTEGRATION_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
