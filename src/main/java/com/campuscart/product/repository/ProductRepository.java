package com.campuscart.product.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campuscart.product.domain.Product;

import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    boolean existsByCategoryId(UUID categoryId);

    Page<Product> findByStatusOrderByCreatedAtDesc(com.campuscart.product.domain.ProductStatus status,
                                                    Pageable pageable);

    Page<Product> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(com.campuscart.product.domain.ProductStatus status);

    long countByCreatedAtAfter(java.time.Instant createdAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from Product product where product.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") UUID id);

    @Query("select distinct p from Product p "
            + "left join fetch p.seller s "
            + "left join fetch p.college c "
            + "left join fetch p.city city "
            + "left join fetch p.category cat "
            + "where p.id in :ids")
    java.util.List<com.campuscart.product.domain.Product> findAllWithAssociationsByIdIn(@Param("ids") java.util.Collection<UUID> ids);
}
