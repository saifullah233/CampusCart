package com.campuscart.catalog.dto;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(UUID id, String name, String slug, boolean active, Instant createdAt, Instant updatedAt) {
}
