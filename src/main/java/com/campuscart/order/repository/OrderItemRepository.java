package com.campuscart.order.repository;

import com.campuscart.order.domain.OrderItem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderIdOrderByCreatedAtAsc(UUID orderId);

    /**
     * Bulk-loads the items for a page of orders in one query, fetching the seller so the
     * response mapper can read {@code seller.fullName} without a per-row lazy lookup.
     * Used by the paged buyer/seller history endpoints to avoid an N+1.
     */
    @Query("select item from OrderItem item join fetch item.seller "
            + "where item.order.id in :orderIds order by item.createdAt asc")
    List<OrderItem> findByOrderIdInFetchSeller(@Param("orderIds") Collection<UUID> orderIds);

    Optional<OrderItem> findByOrderIdAndProductId(UUID orderId, UUID productId);

    boolean existsByOrderIdAndSellerId(UUID orderId, UUID sellerId);
}
