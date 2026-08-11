package com.campuscart.security.login;

import com.campuscart.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "login_rate_limits",
        indexes = {
                @Index(name = "ux_login_rate_limits_identity", columnList = "identity_hash", unique = true),
                @Index(name = "idx_login_rate_limits_locked_until", columnList = "locked_until")
        })
public class LoginRateLimit extends BaseEntity {

    @Column(name = "identity_hash", nullable = false, length = 64, unique = true)
    private String identityHash;

    @Column(name = "window_started_at", nullable = false)
    private Instant windowStartedAt;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    protected LoginRateLimit() {
    }

    public LoginRateLimit(String identityHash, Instant now) {
        this.identityHash = identityHash;
        this.windowStartedAt = now;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void recordFailure(Instant now, Duration window, int maxFailures, Duration lockout) {
        if (!withinWindow(now, window)) {
            windowStartedAt = now;
            failureCount = 0;
            lockedUntil = null;
        }
        failureCount++;
        if (failureCount >= maxFailures) {
            lockedUntil = now.plus(lockout);
        }
    }

    public void clearFailures(Instant now) {
        windowStartedAt = now;
        failureCount = 0;
        lockedUntil = null;
    }

    private boolean withinWindow(Instant now, Duration window) {
        return windowStartedAt != null && windowStartedAt.plus(window).isAfter(now);
    }
}
