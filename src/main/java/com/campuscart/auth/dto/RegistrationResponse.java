package com.campuscart.auth.dto;

import com.campuscart.auth.service.OtpChallengeResponse;

import java.util.UUID;

public record RegistrationResponse(
        UUID userId,
        String status,
        OtpChallengeResponse otp) {
}
