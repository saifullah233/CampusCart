package com.campuscart.notification.service;

import com.campuscart.common.api.PageResponse;
import com.campuscart.common.exception.BusinessRuleException;
import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.notification.domain.Notification;
import com.campuscart.notification.domain.NotificationType;
import com.campuscart.notification.dto.NotificationResponse;
import com.campuscart.notification.repository.NotificationRepository;
import com.campuscart.product.domain.Product;
import com.campuscart.product.domain.ProductStatus;
import com.campuscart.product.domain.SellingReach;
import com.campuscart.user.domain.AccountStatus;
import com.campuscart.user.domain.User;
import com.campuscart.user.repository.UserRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class NotificationService {

    private static final int NEW_PRODUCT_NOTIFICATION_BATCH_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               SimpMessagingTemplate messagingTemplate,
                               Clock clock) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.clock = clock;
    }

    @Transactional
    public NotificationResponse create(UUID userId, NotificationType type, String title,
                                       String content, String dataJson) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        return create(user, type, title, content, dataJson);
    }

    /**
     * Persist + publish for an already-loaded recipient. Fan-out callers that already
     * hold the {@link User} (e.g. the new-product broadcast) use this overload to avoid
     * re-fetching every recipient by id.
     */
    @Transactional
    public NotificationResponse create(User user, NotificationType type, String title,
                                       String content, String dataJson) {
        Notification notification = notificationRepository.save(
                new Notification(user, type, title, content, dataJson));
        NotificationResponse response = toResponse(notification);
        publishAfterCommit(user.getEmail(), response);
        return response;
    }

    @Transactional
    public void notifyNewProduct(Product product) {
        if (product.getStatus() != ProductStatus.ACTIVE) {
            return;
        }
        int page = 0;
        org.springframework.data.domain.Page<User> users;
        do {
            users = userRepository.findByStatus(AccountStatus.ACTIVE,
                    PageRequest.of(page, NEW_PRODUCT_NOTIFICATION_BATCH_SIZE, Sort.by("id")));
            users.stream()
                    .filter(user -> !user.getId().equals(product.getSeller().getId()))
                    .filter(user -> canDiscover(product, user))
                    .forEach(user -> create(user, NotificationType.NEW_PRODUCT, "New product available",
                            product.getTitle() + " is now available.",
                            "{\"productId\":\"" + product.getId() + "\"}"));
            page++;
        } while (users.hasNext());
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UUID userId, int page, int size) {
        validatePage(page, size);
        return PageResponse.from(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", notificationId));
        notification.markRead(clock.instant());
        return toResponse(notification);
    }

    @Transactional
    public long markAllRead(UUID userId) {
        return notificationRepository.markAllUnreadAsRead(userId, clock.instant());
    }

    private boolean canDiscover(Product product, User viewer) {
        if (product.getSellingReach() == SellingReach.CAMPUS_ONLY) {
            if (viewer.getAccountType() == com.campuscart.user.domain.UserType.COMMUNITY || viewer.getCollege() == null) {
                return false;
            }
            return product.getCollege() != null && viewer.getCollege() != null
                    && viewer.getCollege().getId().equals(product.getCollege().getId());
        }
        return product.getSellingReach() == SellingReach.OUTSIDE_CAMPUS;
    }

    private void publishAfterCommit(String username, NotificationResponse response) {
        Runnable publish = () -> messagingTemplate.convertAndSendToUser(
                username, "/queue/notifications", response);
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getTitle(),
                notification.getContent(), notification.getDataJson(), notification.getReadAt() != null,
                notification.getCreatedAt(), notification.getReadAt());
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new BusinessRuleException("Page must be non-negative and size must be between 1 and 50.");
        }
    }
}
