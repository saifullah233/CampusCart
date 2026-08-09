package com.campuscart.security.otp;

import com.campuscart.common.domain.BaseEntity;
import com.campuscart.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** A single, one-time, server-verifiable OTP challenge. */
@Entity
@Table(
        name = "otp_challenges",
        indexes = {
                @Index(name = "idx_otp_challenges_user_id", columnList = "user_id"),
                @Index(name = "idx_otp_challenges_destination_created", columnList = "destination_hash,created_at")
        })
public class OtpChallenge extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_otp_challenges_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 10)
    private OtpChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private OtpPurpose purpose;

    @Column(name = "destination_hash", nullable = false, length = 64)
    private String destinationHash;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "next_resend_at", nullable = false)
    private Instant nextResendAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "superseded_at")
    private Instant supersededAt;

    protected OtpChallenge() {
        // Required by JPA.
    }

    public OtpChallenge(User user,
                        OtpChannel channel,
                        OtpPurpose purpose,
                        String destinationHash,
                        String codeHash,
                        Instant expiresAt,
                        Instant nextResendAt) {
        this.user = user;
        this.channel = channel;
        this.purpose = purpose;
        this.destinationHash = destinationHash;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.nextResendAt = nextResendAt;
    }

    public User getUser() {
        return user;
    }

    public OtpChannel getChannel() {
        return channel;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public String getDestinationHash() {
        return destinationHash;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getNextResendAt() {
        return nextResendAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public Instant getSupersededAt() {
        return supersededAt;
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean isUsable(Instant now, int maxAttempts) {
        return !isVerified()
                && supersededAt == null
                && expiresAt.isAfter(now)
                && attemptCount < maxAttempts;
    }

    public void recordFailedAttempt() {
        attemptCount++;
    }

    public void markVerified(Instant when) {
        verifiedAt = when;
    }

    public void markSuperseded(Instant when) {
        supersededAt = when;
    }
}
