package com.campuscart.cart.dto;

import com.campuscart.product.domain.ProductStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID productId,
        String title,
        UUID sellerId,
        String sellerName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        ProductStatus status,
        int availableQuantity,
        boolean available,
        String imageUrl) {

    public CartItemResponse(UUID productId, String title, UUID sellerId, String sellerName,
                            BigDecimal unitPrice, int quantity, BigDecimal lineTotal,
                            ProductStatus status, int availableQuantity, boolean available) {
        this(productId, title, sellerId, sellerName, unitPrice, quantity, lineTotal, status, availableQuantity, available, null);
    }
}
