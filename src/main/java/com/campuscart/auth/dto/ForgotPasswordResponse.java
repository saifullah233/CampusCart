package com.campuscart.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record ForgotPasswordResponse(
        UUID challengeId,
        String destination,
        Instant expiresAt,
        Instant nextResendAt) {
}
