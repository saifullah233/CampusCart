package com.campuscart.review.repository;

import com.campuscart.review.domain.Review;
import com.campuscart.review.domain.ReviewStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByReviewerIdAndOrderIdAndProductId(UUID reviewerId, UUID orderId, UUID productId);

    @EntityGraph(attributePaths = {"reviewer", "reviewedUser", "product", "order", "moderatedBy"})
    Page<Review> findByProductIdAndStatusOrderByCreatedAtDesc(UUID productId, ReviewStatus status,
                                                                Pageable pageable);

    @EntityGraph(attributePaths = {"reviewer", "reviewedUser", "product", "order", "moderatedBy"})
    Page<Review> findByReviewedUserIdAndStatusOrderByCreatedAtDesc(UUID reviewedUserId, ReviewStatus status,
                                                                     Pageable pageable);

    @EntityGraph(attributePaths = {"reviewer", "reviewedUser", "product", "order", "moderatedBy"})
    Page<Review> findByReviewerIdOrderByCreatedAtDesc(UUID reviewerId, Pageable pageable);

    @EntityGraph(attributePaths = {"reviewer", "reviewedUser", "product", "order", "moderatedBy"})
    Page<Review> findByStatusOrderByCreatedAtAsc(ReviewStatus status, Pageable pageable);

    long countByStatus(ReviewStatus status);

    long countByCreatedAtAfter(java.time.Instant createdAt);
}
