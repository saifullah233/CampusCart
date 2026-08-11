package com.campuscart.admin.service;

import com.campuscart.admin.dto.AdminAnalyticsResponse;
import com.campuscart.chat.domain.ChatReportStatus;
import com.campuscart.chat.repository.ChatMessageRepository;
import com.campuscart.chat.repository.ChatReportRepository;
import com.campuscart.order.domain.OrderStatus;
import com.campuscart.order.repository.OrderRepository;
import com.campuscart.product.domain.ProductStatus;
import com.campuscart.product.repository.ProductRepository;
import com.campuscart.review.repository.ReviewRepository;
import com.campuscart.user.domain.AccountStatus;
import com.campuscart.user.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAnalyticsService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ChatReportRepository reportRepository;
    private final ReviewRepository reviewRepository;
    private final ChatMessageRepository messageRepository;
    private final AdminAccessService adminAccessService;
    private final Clock clock;

    public AdminAnalyticsService(UserRepository userRepository, ProductRepository productRepository,
                                 OrderRepository orderRepository, ChatReportRepository reportRepository,
                                 ReviewRepository reviewRepository, ChatMessageRepository messageRepository,
                                 AdminAccessService adminAccessService, Clock clock) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.reportRepository = reportRepository;
        this.reviewRepository = reviewRepository;
        this.messageRepository = messageRepository;
        this.adminAccessService = adminAccessService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse dashboard(UUID adminId) {
        adminAccessService.requireAdmin(adminId);
        Instant generatedAt = clock.instant();
        Instant recentSince = generatedAt.minus(Duration.ofDays(30));
        long recentProducts = productRepository.countByCreatedAtAfter(recentSince);
        long recentOrders = orderRepository.countByCreatedAtAfter(recentSince);
        long recentReviews = reviewRepository.countByCreatedAtAfter(recentSince);
        long recentMessages = messageRepository.countByCreatedAtAfter(recentSince);
        return new AdminAnalyticsResponse(
                userRepository.count(),
                userRepository.countByStatus(AccountStatus.ACTIVE),
                productRepository.count(),
                productRepository.countByStatus(ProductStatus.ACTIVE),
                productRepository.countByStatus(ProductStatus.SOLD),
                orderRepository.count(),
                orderRepository.countByStatus(OrderStatus.COMPLETED),
                reportRepository.count(),
                reportRepository.countByStatusIn(ChatReportStatus.activeStatuses()),
                recentProducts,
                recentOrders,
                recentReviews,
                recentMessages,
                recentProducts + recentOrders + recentReviews,
                generatedAt);
    }
}
