package com.campuscart.common.exception;

/**
 * Base type for all deliberate, client-facing application exceptions.
 *
 * <p>Carries an {@link ErrorCode} so the global handler can map to the correct HTTP
 * status and a stable error identifier without inspecting exception subtypes.</p>
 */
public class ApiException extends RuntimeException {

    private final transient ErrorCode errorCode;

    protected ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected ApiException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
