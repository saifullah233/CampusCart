package com.campuscart.review.web;

import com.campuscart.common.api.ApiResponse;
import com.campuscart.common.api.PageResponse;
import com.campuscart.review.domain.ReviewStatus;
import com.campuscart.review.dto.ReviewModerationRequest;
import com.campuscart.review.dto.ReviewResponse;
import com.campuscart.review.service.ReviewService;
import com.campuscart.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class ReviewModerationController {

    private final ReviewService reviewService;

    public ReviewModerationController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ReviewResponse>> list(@AuthenticationPrincipal AuthenticatedUser principal,
                                                            @RequestParam(defaultValue = "PENDING") ReviewStatus status,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reviewService.moderationQueue(principal.id(), status, page, size));
    }

    @PatchMapping("/{reviewId}")
    public ApiResponse<ReviewResponse> moderate(@AuthenticationPrincipal AuthenticatedUser principal,
                                                @PathVariable UUID reviewId,
                                                @Valid @RequestBody ReviewModerationRequest request) {
        return ApiResponse.ok(reviewService.moderate(principal.id(), reviewId, request));
    }
}
