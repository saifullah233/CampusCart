package com.campuscart.product.dto;

import com.campuscart.product.domain.ProductStatus;
import java.time.Instant;
import java.util.UUID;

public record AdminProductResponse(UUID id, ProductResponse product, ProductStatus status,
                                   Instant createdAt, Instant updatedAt) {
}
