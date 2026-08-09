package com.campuscart.product.dto;

import com.campuscart.product.domain.MarketplaceScope;
import com.campuscart.product.domain.ProductStatus;
import com.campuscart.product.domain.ProductType;
import com.campuscart.product.domain.SellingReach;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductSearchQuery(
        String keyword,
        UUID categoryId,
        ProductType productType,
        SellingReach sellingReach,
        UUID collegeId,
        UUID cityId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        ProductStatus status,
        MarketplaceScope scope,
        int page,
        int size,
        String sort
) {
}
