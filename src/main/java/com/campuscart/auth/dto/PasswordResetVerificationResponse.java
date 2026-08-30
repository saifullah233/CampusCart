package com.campuscart.auth.dto;

import java.time.Instant;

public record PasswordResetVerificationResponse(
        String resetToken,
        Instant expiresAt) {
}
