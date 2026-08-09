package com.campuscart.payment.dto;

import com.campuscart.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        BigDecimal amount,
        PaymentStatus status,
        String provider,
        String providerPaymentId) {
}
