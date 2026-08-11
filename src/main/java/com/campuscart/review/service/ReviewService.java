package com.campuscart.review.service;

import com.campuscart.common.api.PageResponse;
import com.campuscart.audit.service.AuditLogService;
import com.campuscart.common.exception.DuplicateResourceException;
import com.campuscart.common.exception.InvalidReviewException;
import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.order.domain.Order;
import com.campuscart.order.domain.OrderItem;
import com.campuscart.order.domain.OrderStatus;
import com.campuscart.order.repository.OrderItemRepository;
import com.campuscart.order.repository.OrderRepository;
import com.campuscart.review.domain.Review;
import com.campuscart.review.domain.ReviewStatus;
import com.campuscart.review.dto.CreateReviewRequest;
import com.campuscart.review.dto.ReviewModerationRequest;
import com.campuscart.review.dto.ReviewResponse;
import com.campuscart.review.repository.ReviewRepository;
import com.campuscart.user.domain.User;
import com.campuscart.user.repository.UserRepository;
import com.campuscart.user.service.UserService;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ReviewMapper reviewMapper;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public ReviewService(ReviewRepository reviewRepository, OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository, UserRepository userRepository,
                         UserService userService, ReviewMapper reviewMapper, AuditLogService auditLogService,
                         Clock clock) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.reviewMapper = reviewMapper;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @Transactional
    public ReviewResponse create(UUID reviewerId, CreateReviewRequest request) {
        User reviewer = userService.requireActive(reviewerId);
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> ResourceNotFoundException.of("Order", request.orderId()));
        if (!order.getBuyer().getId().equals(reviewerId)) {
            throw new InvalidReviewException("Only the buyer who completed the order can review it.");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new InvalidReviewException("Reviews require a completed order.");
        }
        OrderItem item = orderItemRepository.findByOrderIdAndProductId(order.getId(), request.productId())
                .orElseThrow(() -> new InvalidReviewException("The product is not part of this order."));
        if (item.getSeller().getId().equals(reviewerId)) {
            throw new InvalidReviewException("You cannot review your own product.");
        }
        if (reviewRepository.existsByReviewerIdAndOrderIdAndProductId(reviewerId, order.getId(), item.getProduct().getId())) {
            throw new DuplicateResourceException("You have already reviewed this product for the order.");
        }
        Review review = new Review(reviewer, item.getSeller(), item.getProduct(), order, request.rating(),
                request.reviewText().trim());
        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> listProduct(UUID productId, int page, int size) {
        validatePage(page, size);
        return PageResponse.from(reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(
                        productId, ReviewStatus.APPROVED, pageRequest(page, size)).map(reviewMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> listSeller(UUID sellerId, int page, int size) {
        if (!userRepository.existsById(sellerId)) {
            throw ResourceNotFoundException.of("User", sellerId);
        }
        validatePage(page, size);
        return PageResponse.from(reviewRepository.findByReviewedUserIdAndStatusOrderByCreatedAtDesc(
                        sellerId, ReviewStatus.APPROVED, pageRequest(page, size)).map(reviewMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> mine(UUID reviewerId, int page, int size) {
        userService.requireActive(reviewerId);
        validatePage(page, size);
        return PageResponse.from(reviewRepository.findByReviewerIdOrderByCreatedAtDesc(reviewerId,
                        pageRequest(page, size)).map(reviewMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ReviewResponse get(UUID viewerId, UUID reviewId) {
        User viewer = userService.requireActive(viewerId);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.of("Review", reviewId));
        if (review.getStatus() != ReviewStatus.APPROVED && !viewer.getRole().isAdmin()
                && !review.getReviewer().getId().equals(viewerId)) {
            throw ResourceNotFoundException.of("Review", reviewId);
        }
        return reviewMapper.toResponse(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> moderationQueue(UUID adminId, ReviewStatus status, int page, int size) {
        requireAdmin(adminId);
        validatePage(page, size);
        return PageResponse.from(reviewRepository.findByStatusOrderByCreatedAtAsc(status, pageRequest(page, size))
                .map(reviewMapper::toResponse));
    }

    @Transactional
    public ReviewResponse moderate(UUID adminId, UUID reviewId, ReviewModerationRequest request) {
        User admin = requireAdmin(adminId);
        if (request.status() == ReviewStatus.PENDING) {
            throw new InvalidReviewException("Review moderation must select an explicit visible or hidden outcome.");
        }
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.of("Review", reviewId));
        review.moderate(admin, request.status(), clock.instant());
        auditLogService.record(admin, "REVIEW_MODERATED", "REVIEW", reviewId,
                "Review status changed to " + request.status().name() + ".");
        return reviewMapper.toResponse(review);
    }

    private User requireAdmin(UUID adminId) {
        User user = userService.requireActive(adminId);
        if (!user.getRole().isAdmin()) {
            throw new AccessDeniedException("Administrator access is required for review moderation.");
        }
        return user;
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidReviewException("Page must be non-negative and size must be between 1 and 50.");
        }
    }
}
