package com.campuscart.wishlist.dto;

import com.campuscart.product.domain.ProductStatus;
import com.campuscart.product.domain.ProductType;
import com.campuscart.product.domain.SellingReach;
import java.math.BigDecimal;
import java.util.UUID;

public record WishlistItemResponse(
        UUID productId,
        String title,
        BigDecimal price,
        ProductType productType,
        SellingReach sellingReach,
        ProductStatus status,
        int availableQuantity,
        String categoryName,
        String collegeName,
        String cityName,
        UUID sellerId,
        String sellerName,
        String imageUrl) {

    public WishlistItemResponse(UUID productId, String title, BigDecimal price,
                                ProductType productType, SellingReach sellingReach,
                                ProductStatus status, int availableQuantity) {
        this(productId, title, price, productType, sellingReach, status, availableQuantity,
                null, null, null, null, null, null);
    }
}
