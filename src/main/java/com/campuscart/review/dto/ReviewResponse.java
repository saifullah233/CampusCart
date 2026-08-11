package com.campuscart.review.dto;

import com.campuscart.review.domain.ReviewStatus;
import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID reviewerId,
        String reviewerName,
        UUID reviewedUserId,
        String reviewedUserName,
        UUID productId,
        UUID orderId,
        int rating,
        String reviewText,
        ReviewStatus status,
        UUID moderatedById,
        Instant moderatedAt,
        Instant createdAt,
        Instant updatedAt) {
}
