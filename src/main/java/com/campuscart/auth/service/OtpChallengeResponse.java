package com.campuscart.auth.service;

import com.campuscart.security.otp.OtpChallenge;

import java.time.Instant;
import java.util.UUID;

public record OtpChallengeResponse(
        UUID challengeId,
        String channel,
        String destination,
        Instant expiresAt,
        Instant nextResendAt) {

    public static OtpChallengeResponse from(OtpChallenge challenge, String destination) {
        return new OtpChallengeResponse(
                challenge.getId(),
                challenge.getChannel().name(),
                OtpService.maskDestination(destination, challenge.getChannel()),
                challenge.getExpiresAt(),
                challenge.getNextResendAt());
    }
}
