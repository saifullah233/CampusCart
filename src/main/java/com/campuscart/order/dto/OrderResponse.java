package com.campuscart.order.dto;

import com.campuscart.order.domain.OrderStatus;
import com.campuscart.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID buyerId,
        BigDecimal totalAmount,
        OrderStatus status,
        PaymentStatus paymentStatus,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}
