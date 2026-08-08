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
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    BUSINESS_RULE_VIOLATION(HttpStatus.CONFLICT),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
