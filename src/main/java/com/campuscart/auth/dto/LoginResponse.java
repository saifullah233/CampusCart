package com.campuscart.auth.dto;

import com.campuscart.auth.service.OtpChallengeResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

/**
 * Payload returned on authentication.
 * For ACTIVE accounts, contains token metadata.
 * For PENDING_VERIFICATION accounts, contains non-secret challenge metadata for email verification.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
        String status,
        UUID userId,
        String accountType,
        Boolean emailVerified,
        OtpChallengeResponse otp,
        OtpChallengeResponse emailOtp,
        AuthTokenResponse tokens,
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn) {

    public static LoginResponse active(AuthTokenResponse tokens) {
        return new LoginResponse(
                "ACTIVE",
                null,
                null,
                null,
                null,
                null,
                tokens,
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.tokenType(),
                tokens.accessTokenExpiresInSeconds());
    }

    public static LoginResponse pendingVerification(
            UUID userId,
            String accountType,
            boolean emailVerified,
            OtpChallengeResponse emailOtp) {
        return new LoginResponse(
                "PENDING_VERIFICATION",
                userId,
                accountType,
                emailVerified,
                emailOtp,
                emailOtp,
                null,
                null,
                null,
                null,
                null);
    }
}
