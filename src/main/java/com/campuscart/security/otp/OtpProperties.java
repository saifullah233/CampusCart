package com.campuscart.security.otp;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** Operational limits for OTP challenges. */
@Validated
@ConfigurationProperties(prefix = "security.otp")
public class OtpProperties {

    @NotNull
    private Duration ttl = Duration.ofMinutes(5);

    @NotNull
    private Duration resendCooldown = Duration.ofSeconds(60);

    @NotNull
    private Duration rateWindow = Duration.ofMinutes(15);

    @Min(1)
    @Max(10)
    private int maxAttempts = 5;

    @Min(1)
    @Max(20)
    private int maxSendsPerWindow = 5;

    @Min(4)
    @Max(8)
    private int codeLength = 6;

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public Duration getResendCooldown() {
        return resendCooldown;
    }

    public void setResendCooldown(Duration resendCooldown) {
        this.resendCooldown = resendCooldown;
    }

    public Duration getRateWindow() {
        return rateWindow;
    }

    public void setRateWindow(Duration rateWindow) {
        this.rateWindow = rateWindow;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getMaxSendsPerWindow() {
        return maxSendsPerWindow;
    }

    public void setMaxSendsPerWindow(int maxSendsPerWindow) {
        this.maxSendsPerWindow = maxSendsPerWindow;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }
}
