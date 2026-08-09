package com.campuscart.common.exception;

public class PaymentIntegrationUnavailableException extends ApiException {

    public PaymentIntegrationUnavailableException() {
        super(ErrorCode.PAYMENT_INTEGRATION_UNAVAILABLE,
                "Payment processing is not connected yet.");
    }
}
