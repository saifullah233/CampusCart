package com.campuscart.college.dto;

import java.time.Instant;
import java.util.UUID;

public record CollegeResponse(UUID id, String name, UUID cityId, String cityName, boolean active,
                              Instant createdAt, Instant updatedAt) {
}
