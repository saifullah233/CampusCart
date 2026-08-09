package com.campuscart.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Writes the standard JSON envelope for authenticated but unauthorized requests. */
@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private static final String MESSAGE = "You do not have permission to perform this action.";

    private final SecurityErrorResponseWriter responseWriter;

    public ApiAccessDeniedHandler(SecurityErrorResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        responseWriter.write(response, HttpServletResponse.SC_FORBIDDEN,
                "ACCESS_DENIED", MESSAGE, request.getRequestURI());
    }
}
