package com.campuscart.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        UUID sellerId,
        String sellerName,
        String productTitle,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        String imageUrl) {

    public OrderItemResponse(UUID id, UUID productId, UUID sellerId, String sellerName,
                             String productTitle, BigDecimal unitPrice, int quantity,
                             BigDecimal lineTotal) {
        this(id, productId, sellerId, sellerName, productTitle, unitPrice, quantity, lineTotal, null);
    }
}
