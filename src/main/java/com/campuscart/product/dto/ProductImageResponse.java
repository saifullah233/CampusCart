package com.campuscart.product.dto;

import java.time.Instant;
import java.util.UUID;

public record ProductImageResponse(
        UUID id,
        String url,
        String imageUrl,
        String storageKey,
        String contentType,
        long sizeBytes,
        int displayOrder,
        boolean isCover,
        Instant createdAt
) {
    public ProductImageResponse(UUID id, String url, String contentType, long sizeBytes, Instant createdAt) {
        this(id, url, url, null, contentType, sizeBytes, 0, false, createdAt);
    }
}
