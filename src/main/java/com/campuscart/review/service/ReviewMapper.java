package com.campuscart.review.service;

import com.campuscart.review.domain.Review;
import com.campuscart.review.dto.ReviewResponse;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(Review review) {
        return new ReviewResponse(review.getId(), review.getReviewer().getId(), review.getReviewer().getFullName(),
                review.getReviewedUser().getId(), review.getReviewedUser().getFullName(),
                review.getProduct().getId(), review.getOrder().getId(), review.getRating(), review.getReviewText(),
                review.getStatus(), review.getModeratedBy() == null ? null : review.getModeratedBy().getId(),
                review.getModeratedAt(), review.getCreatedAt(), review.getUpdatedAt());
    }
}
