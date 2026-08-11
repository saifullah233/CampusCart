package com.campuscart.product.repository;

import com.campuscart.product.domain.ProductLike;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductLikeRepository extends JpaRepository<ProductLike, UUID> {

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    Optional<ProductLike> findByUserIdAndProductId(UUID userId, UUID productId);
}
