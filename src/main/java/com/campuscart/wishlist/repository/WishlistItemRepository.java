package com.campuscart.wishlist.repository;

import com.campuscart.wishlist.domain.WishlistItem;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    Optional<WishlistItem> findByUserIdAndProductId(UUID userId, UUID productId);

    Page<WishlistItem> findByUserId(UUID userId, Pageable pageable);
}
