package com.campuscart.payment.service;

import com.campuscart.common.exception.PaymentIntegrationUnavailableException;
import com.campuscart.order.domain.Order;
import com.campuscart.payment.domain.Payment;
import org.springframework.stereotype.Component;

@Component
public class UnavailablePaymentGateway implements PaymentGateway {

    @Override
    public PaymentInitialization initialize(Order order, Payment payment) {
        throw new PaymentIntegrationUnavailableException();
    }
}
