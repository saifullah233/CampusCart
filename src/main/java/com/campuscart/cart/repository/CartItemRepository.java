package com.campuscart.cart.repository;

import com.campuscart.cart.domain.CartItem;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    Optional<CartItem> findByUserIdAndProductId(UUID userId, UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from CartItem item where item.user.id = :userId and item.product.id = :productId")
    Optional<CartItem> findByUserIdAndProductIdForUpdate(@Param("userId") UUID userId,
                                                         @Param("productId") UUID productId);

    List<CartItem> findByUserIdOrderByCreatedAtAsc(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from CartItem item where item.user.id = :userId order by item.createdAt, item.id")
    List<CartItem> findByUserIdForUpdate(@Param("userId") UUID userId);

    void deleteByUserIdAndProductId(UUID userId, UUID productId);
}
