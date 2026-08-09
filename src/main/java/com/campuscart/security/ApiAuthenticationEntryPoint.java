package com.campuscart.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Writes the standard JSON envelope for unauthenticated API requests. */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String MESSAGE = "Authentication is required to access this resource.";

    private final SecurityErrorResponseWriter responseWriter;

    public ApiAuthenticationEntryPoint(SecurityErrorResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        responseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED", MESSAGE, request.getRequestURI());
    }
}
