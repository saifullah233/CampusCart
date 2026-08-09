package com.campuscart.payment.domain;

public enum PaymentStatus {
    NOT_CONNECTED,
    PENDING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    REFUNDED
}
