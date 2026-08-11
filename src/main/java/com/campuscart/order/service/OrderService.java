package com.campuscart.order.service;

import com.campuscart.cart.domain.CartItem;
import com.campuscart.cart.repository.CartItemRepository;
import com.campuscart.common.api.PageResponse;
import com.campuscart.common.exception.CartEmptyException;
import com.campuscart.common.exception.BusinessRuleException;
import com.campuscart.common.exception.ProductUnavailableException;
import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.common.exception.OrderStateException;
import com.campuscart.order.domain.Order;
import com.campuscart.order.domain.OrderItem;
import com.campuscart.order.domain.OrderStatus;
import com.campuscart.order.dto.OrderItemResponse;
import com.campuscart.order.dto.OrderResponse;
import com.campuscart.order.repository.OrderItemRepository;
import com.campuscart.order.repository.OrderRepository;
import com.campuscart.notification.domain.NotificationType;
import com.campuscart.notification.service.NotificationService;
import com.campuscart.payment.domain.Payment;
import com.campuscart.payment.repository.PaymentRepository;
import com.campuscart.product.domain.Product;
import com.campuscart.product.domain.ProductStatus;
import com.campuscart.product.repository.ProductRepository;
import com.campuscart.user.domain.User;
import com.campuscart.user.service.UserService;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final CartItemRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    public OrderService(CartItemRepository cartRepository,
                        ProductRepository productRepository,
                        OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        PaymentRepository paymentRepository,
                        UserService userService,
                        NotificationService notificationService) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @Transactional
    public OrderResponse createFromCart(UUID buyerId) {
        User buyer = userService.requireActive(buyerId);
        List<CartItem> cartItems = cartRepository.findByUserIdForUpdate(buyerId);
        if (cartItems.isEmpty()) {
            throw new CartEmptyException();
        }

        List<CartItem> orderedItems = cartItems.stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .toList();
        BigDecimal total = BigDecimal.ZERO;
        List<LockedPurchase> purchases = new java.util.ArrayList<>();
        for (CartItem cartItem : orderedItems) {
            Product product = productRepository.findByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Product", cartItem.getProduct().getId()));
            if (product.getSeller().getId().equals(buyerId)) {
                throw new ProductUnavailableException("You cannot purchase your own product.");
            }
            if (cartItem.getQuantity() < 1 || product.getStatus() != ProductStatus.ACTIVE
                    || product.getQuantity() < cartItem.getQuantity()) {
                throw new ProductUnavailableException("One or more cart products are unavailable.");
            }
            product.reserveQuantity(cartItem.getQuantity());
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            purchases.add(new LockedPurchase(product, cartItem.getQuantity()));
        }

        Order order = orderRepository.save(new Order(buyer, total));
        for (LockedPurchase purchase : purchases) {
            orderItemRepository.save(new OrderItem(order, purchase.product(), purchase.quantity()));
        }
        paymentRepository.save(new Payment(order, total));
        purchases.stream().map(purchase -> purchase.product().getSeller().getId()).distinct()
                .forEach(sellerId -> notificationService.create(sellerId, NotificationType.ORDER_RECEIVED,
                        "Order received", "A buyer placed an order for your product.",
                        "{\"orderId\":\"" + order.getId() + "\"}"));
        cartRepository.deleteAllInBatch(cartItems);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> buyerHistory(UUID buyerId, int page, int size) {
        userService.requireActive(buyerId);
        return pageResponse(orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId, pageRequest(page, size)));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> sellerHistory(UUID sellerId, int page, int size) {
        userService.requireActive(sellerId);
        return pageResponse(orderRepository.findSellerOrders(sellerId, pageRequest(page, size)));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID principalId, UUID orderId) {
        User principal = userService.requireActive(principalId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));
        ensureCanView(principal, order);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse transition(UUID principalId, UUID orderId, OrderStatus target) {
        User principal = userService.requireActive(principalId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));
        authorizeTransition(principal, order, target);
        try {
            order.transitionTo(target);
        } catch (IllegalStateException ex) {
            throw new OrderStateException(ex.getMessage());
        }
        if (target == OrderStatus.CANCELLED || target == OrderStatus.REJECTED) {
            releaseReservedStock(orderId);
        }
        notificationService.create(order.getBuyer().getId(), NotificationType.ORDER_UPDATE,
                "Order updated", "Your order is now " + target.name() + ".",
                "{\"orderId\":\"" + orderId + "\",\"status\":\"" + target.name() + "\"}");
        orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(item -> item.getSeller().getId())
                .distinct()
                .forEach(sellerId -> notificationService.create(sellerId, NotificationType.ORDER_UPDATE,
                        "Order updated", "An order containing your product is now " + target.name() + ".",
                        "{\"orderId\":\"" + orderId + "\",\"status\":\"" + target.name() + "\"}"));
        return toResponse(order);
    }

    private void releaseReservedStock(UUID orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .toList();
        for (OrderItem item : items) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Product", item.getProduct().getId()));
            product.restoreQuantity(item.getQuantity());
        }
    }

    private void ensureCanView(User principal, Order order) {
        if (principal.getRole().isAdmin() || order.getBuyer().getId().equals(principal.getId())
                || orderItemRepository.existsByOrderIdAndSellerId(order.getId(), principal.getId())) {
            return;
        }
        throw new AccessDeniedException("Order access is restricted to its buyer and sellers.");
    }

    private void authorizeTransition(User principal, Order order, OrderStatus target) {
        if (principal.getRole().isAdmin()) {
            return;
        }
        if (order.getBuyer().getId().equals(principal.getId())) {
            if (target != OrderStatus.CANCELLED && target != OrderStatus.COMPLETED) {
                throw new AccessDeniedException("Buyers may only cancel or complete their orders.");
            }
            return;
        }
        if (!orderItemRepository.existsByOrderIdAndSellerId(order.getId(), principal.getId())) {
            throw new AccessDeniedException("Seller access is required for this order.");
        }
        if (target != OrderStatus.ACCEPTED && target != OrderStatus.REJECTED
                && target != OrderStatus.SHIPPED && target != OrderStatus.DELIVERED) {
            throw new AccessDeniedException("Sellers cannot perform this order transition.");
        }
    }

    private PageResponse<OrderResponse> pageResponse(org.springframework.data.domain.Page<Order> page) {
        List<UUID> orderIds = page.getContent().stream().map(Order::getId).toList();
        // Bulk-load items (with seller fetched) and payments for the whole page so the
        // mapper does not fire a per-order query — avoids an N+1 on the history endpoints.
        Map<UUID, List<OrderItem>> itemsByOrder = orderIds.isEmpty() ? Map.of()
                : orderItemRepository.findByOrderIdInFetchSeller(orderIds).stream()
                        .collect(Collectors.groupingBy(item -> item.getOrder().getId()));
        Map<UUID, Payment> paymentByOrder = orderIds.isEmpty() ? Map.of()
                : paymentRepository.findByOrderIdIn(orderIds).stream()
                        .collect(Collectors.toMap(payment -> payment.getOrder().getId(), payment -> payment));
        return PageResponse.from(page.map(order -> toResponse(order,
                itemsByOrder.getOrDefault(order.getId(), List.of()),
                paymentByOrder.get(order.getId()))));
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new BusinessRuleException("Page must be non-negative and size must be between 1 and 50.");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByCreatedAtAsc(order.getId());
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Payment", order.getId()));
        return toResponse(order, items, payment);
    }

    private OrderResponse toResponse(Order order, List<OrderItem> items, Payment payment) {
        if (payment == null) {
            throw ResourceNotFoundException.of("Payment", order.getId());
        }
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> new OrderItemResponse(item.getId(), item.getProduct().getId(), item.getSeller().getId(),
                        item.getSeller().getFullName(), item.getProductTitle(), item.getUnitPrice(), item.getQuantity(),
                        item.getLineTotal()))
                .toList();
        return new OrderResponse(order.getId(), order.getBuyer().getId(), order.getTotalAmount(), order.getStatus(),
                payment.getStatus(), itemResponses, order.getCreatedAt(), order.getUpdatedAt(), order.getVersion());
    }

    private record LockedPurchase(Product product, int quantity) { }
}
