package com.campuscart.product.repository;

import com.campuscart.product.domain.ProductImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProductIdOrderByCreatedAtAsc(UUID productId);

    Optional<ProductImage> findByIdAndProductId(UUID imageId, UUID productId);

    long countByProductId(UUID productId);
}
