package com.campuscart.auth.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ResendOtpRequest(@NotNull UUID challengeId) {
}
