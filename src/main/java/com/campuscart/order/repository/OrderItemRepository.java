package com.campuscart.order.repository;

import com.campuscart.order.domain.OrderItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderIdOrderByCreatedAtAsc(UUID orderId);

    Optional<OrderItem> findByOrderIdAndProductId(UUID orderId, UUID productId);

    boolean existsByOrderIdAndSellerId(UUID orderId, UUID sellerId);
}
