package com.campuscart.order.repository;

import com.campuscart.order.domain.Order;
import com.campuscart.order.domain.OrderStatus;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select purchaseOrder from Order purchaseOrder where purchaseOrder.id = :id")
    java.util.Optional<Order> findByIdForUpdate(@Param("id") UUID id);

    Page<Order> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId, Pageable pageable);

    @Query(
            value = "select o from Order o where exists (select 1 from OrderItem item where item.order = o and item.seller.id = :sellerId)",
            countQuery = "select count(o) from Order o where exists (select 1 from OrderItem item where item.order = o and item.seller.id = :sellerId)")
    Page<Order> findSellerOrders(@Param("sellerId") UUID sellerId, Pageable pageable);

    long countByStatus(OrderStatus status);

    long countByCreatedAtAfter(java.time.Instant createdAt);
}
