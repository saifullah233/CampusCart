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

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<Order> findById(UUID id);

    Page<Order> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId, Pageable pageable);

    @Query("select distinct item.order from OrderItem item "
            + "where item.seller.id = :sellerId order by item.order.createdAt desc")
    Page<Order> findSellerOrders(@Param("sellerId") UUID sellerId, Pageable pageable);

    long countByStatus(OrderStatus status);

    long countByCreatedAtAfter(java.time.Instant createdAt);
}
