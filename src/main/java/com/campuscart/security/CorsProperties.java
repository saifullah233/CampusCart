package com.campuscart.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * Explicit CORS policy for browser clients.
 *
 * <p>Origins are configured, never reflected from the request. Wildcards are
 * rejected by {@link SecurityConfig} so adding credentials later cannot turn
 * an overly broad policy into an accidental data exposure.</p>
 */
@Validated
@ConfigurationProperties(prefix = "security.cors")
public class CorsProperties {

    @NotEmpty
    private List<@NotBlank String> allowedOrigins = new ArrayList<>();

    @NotEmpty
    private List<@NotBlank String> allowedMethods = new ArrayList<>();

    @NotEmpty
    private List<@NotBlank String> allowedHeaders = new ArrayList<>();

    public List<String> getAllowedOrigins() {
        return List.copyOf(allowedOrigins);
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = new ArrayList<>(allowedOrigins);
    }

    public List<String> getAllowedMethods() {
        return List.copyOf(allowedMethods);
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = new ArrayList<>(allowedMethods);
    }

    public List<String> getAllowedHeaders() {
        return List.copyOf(allowedHeaders);
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = new ArrayList<>(allowedHeaders);
    }
}
