package com.campuscart.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Uniform response envelope returned by every CampusCart endpoint.
 *
 * <p>JPA entities are never serialized directly; controllers return DTOs wrapped in
 * this envelope. {@code null} members are omitted from the JSON payload.</p>
 *
 * @param <T> the payload type on success
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        ApiError error,
        Instant timestamp
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data, null, Instant.now());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> failure(String message, ApiError error) {
        return new ApiResponse<>(false, message, null, error, Instant.now());
    }
}
