package com.campuscart.payment.service;

import com.campuscart.order.domain.Order;
import com.campuscart.payment.domain.Payment;

/** Provider boundary. Implementations must return a provider-confirmed state. */
public interface PaymentGateway {

    PaymentInitialization initialize(Order order, Payment payment);
}
