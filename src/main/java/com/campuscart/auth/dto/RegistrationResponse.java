package com.campuscart.auth.dto;

import com.campuscart.auth.service.OtpChallengeResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RegistrationResponse(
        UUID userId,
        String status,
        OtpChallengeResponse otp,
        OtpChallengeResponse emailOtp) {

    public RegistrationResponse(UUID userId, String status, OtpChallengeResponse otp) {
        this(userId, status, otp, otp);
    }
}
