package com.campuscart.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record VerifyPasswordResetOtpRequest(
        @NotNull UUID challengeId,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Verification code must be 6 digits") String code) {
}
