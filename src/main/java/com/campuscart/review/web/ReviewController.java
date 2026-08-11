package com.campuscart.review.web;

import com.campuscart.common.api.ApiResponse;
import com.campuscart.common.api.PageResponse;
import com.campuscart.review.dto.CreateReviewRequest;
import com.campuscart.review.dto.ReviewResponse;
import com.campuscart.review.service.ReviewService;
import com.campuscart.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> create(@AuthenticationPrincipal AuthenticatedUser principal,
                                                               @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Review submitted for moderation.", reviewService.create(principal.id(), request)));
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<PageResponse<ReviewResponse>> productReviews(@PathVariable UUID productId,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reviewService.listProduct(productId, page, size));
    }

    @GetMapping("/sellers/{sellerId}")
    public ApiResponse<PageResponse<ReviewResponse>> sellerReviews(@PathVariable UUID sellerId,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reviewService.listSeller(sellerId, page, size));
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<ReviewResponse>> mine(@AuthenticationPrincipal AuthenticatedUser principal,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reviewService.mine(principal.id(), page, size));
    }

    @GetMapping("/{reviewId}")
    public ApiResponse<ReviewResponse> get(@AuthenticationPrincipal AuthenticatedUser principal,
                                           @PathVariable UUID reviewId) {
        return ApiResponse.ok(reviewService.get(principal.id(), reviewId));
    }
}
