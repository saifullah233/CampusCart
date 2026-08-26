package com.campuscart.product.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campuscart.product.domain.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProductIdOrderByDisplayOrderAscCreatedAtAsc(UUID productId);

    List<ProductImage> findByProductIdOrderByCreatedAtAsc(UUID productId);

    List<ProductImage> findByProductIdInOrderByProductIdAscDisplayOrderAscCreatedAtAsc(java.util.Collection<UUID> productIds);

    List<ProductImage> findByProductIdInOrderByProductIdAscCreatedAtAsc(java.util.Collection<UUID> productIds);

    Optional<ProductImage> findByIdAndProductId(UUID imageId, UUID productId);

    long countByProductId(UUID productId);
}
