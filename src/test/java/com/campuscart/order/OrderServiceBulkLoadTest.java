package com.campuscart.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campuscart.cart.repository.CartItemRepository;
import com.campuscart.notification.service.NotificationService;
import com.campuscart.order.domain.Order;
import com.campuscart.order.domain.OrderItem;
import com.campuscart.order.repository.OrderItemRepository;
import com.campuscart.order.repository.OrderRepository;
import com.campuscart.order.service.OrderService;
import com.campuscart.payment.domain.Payment;
import com.campuscart.payment.repository.PaymentRepository;
import com.campuscart.product.repository.ProductImageRepository;
import com.campuscart.product.repository.ProductRepository;
import com.campuscart.user.domain.User;
import com.campuscart.user.service.UserService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Regression guard for the order-history N+1 fix: a page of orders must be mapped from
 * two bulk queries (items-with-seller + payments), never one item/payment query per row.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceBulkLoadTest {

    @Mock CartItemRepository cartRepository;
    @Mock ProductRepository productRepository;
    @Mock ProductImageRepository productImageRepository;
    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock UserService userService;
    @Mock NotificationService notificationService;

    OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(cartRepository, productRepository, productImageRepository,
                orderRepository, orderItemRepository, paymentRepository, userService, notificationService);
    }

    @Test
    void buyerHistory_bulkLoadsItemsAndPayments_withoutPerOrderQueries() {
        UUID buyerId = UUID.randomUUID();
        UUID firstOrderId = UUID.randomUUID();
        UUID secondOrderId = UUID.randomUUID();

        when(userService.requireActive(buyerId)).thenReturn(mock(User.class));

        Order first = mock(Order.class, RETURNS_DEEP_STUBS);
        when(first.getId()).thenReturn(firstOrderId);
        Order second = mock(Order.class, RETURNS_DEEP_STUBS);
        when(second.getId()).thenReturn(secondOrderId);
        var page = new PageImpl<>(List.of(first, second), PageRequest.of(0, 20), 2);
        when(orderRepository.findByBuyerIdOrderByCreatedAtDesc(eq(buyerId), any())).thenReturn(page);

        OrderItem item = mock(OrderItem.class, RETURNS_DEEP_STUBS);
        when(item.getOrder().getId()).thenReturn(firstOrderId);
        when(orderItemRepository.findByOrderIdInFetchSeller(List.of(firstOrderId, secondOrderId)))
                .thenReturn(List.of(item));

        Payment firstPayment = mock(Payment.class, RETURNS_DEEP_STUBS);
        when(firstPayment.getOrder().getId()).thenReturn(firstOrderId);
        Payment secondPayment = mock(Payment.class, RETURNS_DEEP_STUBS);
        when(secondPayment.getOrder().getId()).thenReturn(secondOrderId);
        when(paymentRepository.findByOrderIdIn(List.of(firstOrderId, secondOrderId)))
                .thenReturn(List.of(firstPayment, secondPayment));

        orderService.buyerHistory(buyerId, 0, 20);

        // Exactly one bulk query for items and one for payments, regardless of page size.
        verify(orderItemRepository).findByOrderIdInFetchSeller(List.of(firstOrderId, secondOrderId));
        verify(paymentRepository).findByOrderIdIn(List.of(firstOrderId, secondOrderId));
        // The per-order (N+1) accessors must not be used on the list path.
        verify(orderItemRepository, never()).findByOrderIdOrderByCreatedAtAsc(any());
        verify(paymentRepository, never()).findByOrderId(any());
    }
}
