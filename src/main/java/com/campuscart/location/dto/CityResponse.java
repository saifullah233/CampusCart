package com.campuscart.location.dto;

import java.time.Instant;
import java.util.UUID;

public record CityResponse(UUID id, String name, String state, boolean active,
                           Instant createdAt, Instant updatedAt) {
}
