package com.campuscart.security.otp;

import com.campuscart.common.domain.BaseEntity;
import com.campuscart.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Short-lived, single-use proof of successful OTP verification for password reset.
 * Stores only a SHA-256 digest of the unguessable token issued to the client.
 */
@Entity
@Table(
        name = "password_reset_authorizations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_password_reset_token_hash",
                columnNames = "token_hash"),
        indexes = {
                @Index(name = "idx_password_reset_user_id", columnList = "user_id"),
                @Index(name = "idx_password_reset_expires_at", columnList = "expires_at")
        })
public class PasswordResetAuthorization extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_password_reset_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_password_reset_challenge"))
    private OtpChallenge challenge;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    protected PasswordResetAuthorization() {
        // Required by JPA.
    }

    public PasswordResetAuthorization(User user,
                                      OtpChallenge challenge,
                                      String tokenHash,
                                      Instant expiresAt) {
        this.user = user;
        this.challenge = challenge;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public User getUser() {
        return user;
    }

    public OtpChallenge getChallenge() {
        return challenge;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isValid(Instant now) {
        return !isUsed() && expiresAt.isAfter(now);
    }

    public void markUsed(Instant when) {
        this.usedAt = when;
    }
}
