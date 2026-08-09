package com.campuscart.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Strongly-typed JWT / token configuration bound from {@code security.jwt.*}.
 *
 * <p>The signing secret is intentionally required with no default: the application must
 * fail to start if {@code JWT_SECRET} is not supplied, rather than fall back to a baked-in
 * key. This enforces the "no secrets in source" rule at the framework level.</p>
 */
@Validated
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /** Issuer claim ({@code iss}) stamped on every access token and verified on parse. */
    @NotBlank
    private String issuer = "campuscart";

    /**
     * Signing secret for HMAC-SHA256; must carry at least 256 bits (32 bytes) of entropy.
     * Supplied exclusively via the {@code JWT_SECRET} environment variable — never a default.
     */
    @NotBlank
    @Size(min = 32, message = "JWT secret must contain at least 32 characters")
    private String secret;

    /** Time-to-live for access tokens. Kept short; refresh tokens carry longevity. */
    @NotNull
    private Duration accessTokenTtl = Duration.ofMinutes(15);

    /** Time-to-live for refresh tokens. */
    @NotNull
    private Duration refreshTokenTtl = Duration.ofDays(14);

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }
}
