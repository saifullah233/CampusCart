package com.campuscart.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record VerifyOtpRequest(
        @NotNull UUID challengeId,
        @NotBlank @Pattern(regexp = "^\\d{4,8}$") String code) {
}
