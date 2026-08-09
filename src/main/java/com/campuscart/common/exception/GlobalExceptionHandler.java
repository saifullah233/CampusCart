package com.campuscart.common.exception;

import com.campuscart.common.api.ApiError;
import com.campuscart.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * Centralized translation of exceptions into safe {@link ApiResponse} payloads.
 *
 * <p>Contract: client errors (4xx) are logged at WARN with their message only; server
 * errors (5xx) are logged at ERROR with the full stack trace server-side, but the
 * response body never exposes stack traces, causes, or internal details.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles all deliberate application exceptions carrying an {@link ErrorCode}.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        log.warn("Handled application exception [{}] on {} {}",
                code, request.getMethod(), request.getRequestURI());
        ApiError error = new ApiError(code.name(), ex.getMessage(), request.getRequestURI(), null);
        return ResponseEntity.status(code.status()).body(ApiResponse.failure(ex.getMessage(), error));
    }

    /**
     * Handles constraint violations on @Validated method parameters / path & query params.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex,
                                                                        HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(v -> new ApiError.FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        log.warn("Constraint violation on {} {}: {} field(s)",
                request.getMethod(), request.getRequestURI(), violations.size());
        ApiError error = new ApiError(ErrorCode.VALIDATION_ERROR.name(),
                "Request validation failed.", request.getRequestURI(), violations);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.status())
                .body(ApiResponse.failure("Request validation failed.", error));
    }

    /**
     * Database-level integrity failure (e.g. unique/foreign-key violation) that wasn't
     * pre-empted by an application check. The DB's own message can leak schema details,
     * so it is logged server-side only and never returned to the client.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                           HttpServletRequest request) {
        log.warn("Data integrity violation on {} {}",
                request.getMethod(), request.getRequestURI());
        ApiError error = new ApiError(ErrorCode.CONSTRAINT_VIOLATION.name(),
                "The request conflicts with the current state of the resource.",
                request.getRequestURI(), null);
        return ResponseEntity.status(ErrorCode.CONSTRAINT_VIOLATION.status())
                .body(ApiResponse.failure("The request conflicts with the current state of the resource.", error));
    }

    /**
     * Authentication failures surfaced through the MVC layer (e.g. a login handler).
     * Filter-chain authentication failures are handled earlier by the
     * {@code AuthenticationEntryPoint}; this is the defence-in-depth path.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex,
                                                                   HttpServletRequest request) {
        log.warn("Authentication failure on {} {}", request.getMethod(), request.getRequestURI());
        ApiError error = new ApiError(ErrorCode.AUTHENTICATION_REQUIRED.name(),
                "Authentication is required to access this resource.", request.getRequestURI(), null);
        return ResponseEntity.status(ErrorCode.AUTHENTICATION_REQUIRED.status())
                .body(ApiResponse.failure("Authentication is required to access this resource.", error));
    }

    /**
     * Authorization failures raised by method security ({@code @PreAuthorize}) that
     * propagate through the dispatcher. URL-level denials are handled earlier by the
     * {@code AccessDeniedHandler}; both paths produce this same uniform envelope.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex,
                                                                 HttpServletRequest request) {
        log.warn("Access denied on {} {}", request.getMethod(), request.getRequestURI());
        ApiError error = new ApiError(ErrorCode.ACCESS_DENIED.name(),
                "You do not have permission to perform this action.", request.getRequestURI(), null);
        return ResponseEntity.status(ErrorCode.ACCESS_DENIED.status())
                .body(ApiResponse.failure("You do not have permission to perform this action.", error));
    }

    /**
     * Last-resort handler. Anything reaching here is unexpected: log fully, return a safe body.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        ApiError error = new ApiError(ErrorCode.INTERNAL_ERROR.name(),
                "An unexpected error occurred.", request.getRequestURI(), null);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status())
                .body(ApiResponse.failure("An unexpected error occurred.", error));
    }

    // --- Overrides of Spring MVC's built-in handlers to keep the response envelope uniform ---

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<ApiError.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldViolation)
                .toList();
        ApiError error = new ApiError(ErrorCode.VALIDATION_ERROR.name(),
                "Request validation failed.", path(request), violations);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.status())
                .body(ApiResponse.failure("Request validation failed.", error));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ApiError error = new ApiError(ErrorCode.MALFORMED_REQUEST.name(),
                "Request body is missing or malformed.", path(request), null);
        return ResponseEntity.status(ErrorCode.MALFORMED_REQUEST.status())
                .body(ApiResponse.failure("Request body is missing or malformed.", error));
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                         HttpHeaders headers,
                                                                         HttpStatusCode status,
                                                                         WebRequest request) {
        ApiError error = new ApiError(ErrorCode.METHOD_NOT_ALLOWED.name(),
                ex.getMessage(), path(request), null);
        return ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOWED.status())
                .body(ApiResponse.failure("HTTP method not supported for this endpoint.", error));
    }

    private ApiError.FieldViolation toFieldViolation(FieldError fieldError) {
        String message = fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : "invalid value";
        return new ApiError.FieldViolation(fieldError.getField(), message);
    }

    private String path(WebRequest request) {
        String description = request.getDescription(false);
        return description.startsWith("uri=") ? description.substring(4) : description;
    }
}
