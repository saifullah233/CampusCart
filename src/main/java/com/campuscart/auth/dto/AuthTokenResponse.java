package com.campuscart.auth.dto;

import java.time.Instant;

public record AuthTokenResponse(
        String tokenType,
        String accessToken,
        long accessTokenExpiresInSeconds,
        String refreshToken,
        Instant refreshTokenExpiresAt) {
}
