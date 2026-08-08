package com.campuscart.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Structured error payload carried by {@link ApiResponse#error()}.
 *
 * <p>The {@code code} is a stable, machine-readable identifier (see
 * {@code com.campuscart.common.exception.ErrorCode}); {@code detail} is a
 * human-readable, non-sensitive message. Stack traces are never included.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String detail,
        String path,
        List<FieldViolation> fieldErrors
) {

    public record FieldViolation(String field, String message) {
    }
}
