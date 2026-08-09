package com.campuscart.user.dto;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String fullName,
        String phoneNumber,
        UUID cityId,
        String cityName,
        String cityState,
        UUID collegeId,
        String collegeName,
        String accountType,
        String role,
        String status,
        boolean emailVerified,
        boolean phoneVerified) {
}
