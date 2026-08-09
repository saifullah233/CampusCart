package com.campuscart.payment.service;

import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.order.domain.Order;
import com.campuscart.order.repository.OrderItemRepository;
import com.campuscart.order.repository.OrderRepository;
import com.campuscart.payment.domain.Payment;
import com.campuscart.payment.dto.PaymentResponse;
import com.campuscart.payment.repository.PaymentRepository;
import com.campuscart.user.domain.User;
import com.campuscart.user.service.UserService;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserService userService;

    public PaymentService(PaymentGateway paymentGateway,
                          PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          OrderItemRepository orderItemRepository,
                          UserService userService) {
        this.paymentGateway = paymentGateway;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userService = userService;
    }

    @Transactional
    public PaymentResponse initialize(UUID principalId, UUID orderId) {
        User principal = userService.requireActive(principalId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));
        if (!principal.getRole().isAdmin() && !order.getBuyer().getId().equals(principalId)) {
            throw new AccessDeniedException("Only the buyer may initialize payment.");
        }
        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Payment", orderId));
        PaymentInitialization initialization = paymentGateway.initialize(order, payment);
        payment.recordProviderInitialization(initialization.provider(), initialization.providerPaymentId(),
                initialization.status());
        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrder().getId(), payment.getAmount(),
                payment.getStatus(), payment.getProvider(), payment.getProviderPaymentId());
    }
}
