package com.campuscart.payment.service;

import com.campuscart.payment.domain.PaymentStatus;

public record PaymentInitialization(String provider, String providerPaymentId, PaymentStatus status) {
}
