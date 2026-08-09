package com.campuscart.product.dto;

import java.time.Instant;
import java.util.UUID;

public record ProductImageResponse(
        UUID id,
        String url,
        String contentType,
        long sizeBytes,
        Instant createdAt
) {
}
