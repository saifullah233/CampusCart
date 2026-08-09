package com.campuscart.product.dto;

import com.campuscart.product.domain.ProductStatus;
import com.campuscart.product.domain.ProductType;
import com.campuscart.product.domain.SellingReach;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID sellerId,
        String sellerName,
        UUID collegeId,
        String collegeName,
        UUID cityId,
        String cityName,
        UUID categoryId,
        String categoryName,
        String categorySlug,
        String title,
        String description,
        BigDecimal price,
        ProductType productType,
        SellingReach sellingReach,
        Integer quantity,
        ProductStatus status,
        List<ProductImageResponse> images,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
}
