package com.campuscart.auth.dto;

import com.campuscart.user.dto.UserProfileResponse;

public record VerificationResponse(
        AuthTokenResponse tokens,
        UserProfileResponse user) {
}
