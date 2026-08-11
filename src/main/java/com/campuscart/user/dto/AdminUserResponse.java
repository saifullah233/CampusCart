package com.campuscart.user.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(UUID id, String email, String fullName, String phoneNumber, String role,
                                String status, String accountType, UUID cityId, String cityName,
                                UUID collegeId, String collegeName, Instant createdAt, Instant updatedAt) {
}
