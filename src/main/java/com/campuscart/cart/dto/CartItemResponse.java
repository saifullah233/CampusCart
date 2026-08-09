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
        boolean available) {
}
