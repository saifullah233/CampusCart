package com.campuscart.security;

import com.campuscart.common.api.ApiError;
import com.campuscart.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Shared response writer because filter-chain failures bypass {@code @RestControllerAdvice}. */
@Component
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response,
                      int status,
                      String code,
                      String message,
                      String path) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        ApiError error = new ApiError(code, message, path, null);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(message, error));
    }
}
