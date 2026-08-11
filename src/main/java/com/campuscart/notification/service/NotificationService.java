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
import java.time.Instant;
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
        userRepository.findByStatus(AccountStatus.ACTIVE).stream()
                .filter(user -> !user.getId().equals(product.getSeller().getId()))
                .filter(user -> canDiscover(product, user))
                .forEach(user -> create(user.getId(), NotificationType.NEW_PRODUCT, "New product available",
                        product.getTitle() + " is now available.", "{\"productId\":\"" + product.getId() + "\"}"));
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
        var unread = notificationRepository.findByUserIdAndReadAtIsNull(userId);
        Instant now = clock.instant();
        unread.forEach(notification -> notification.markRead(now));
        return unread.size();
    }

    private boolean canDiscover(Product product, User viewer) {
        boolean sameCity = product.getCity().getId().equals(viewer.getCity().getId());
        boolean sameCollege = viewer.getCollege() != null && product.getCollege() != null
                && viewer.getCollege().getId().equals(product.getCollege().getId());
        return product.getSellingReach() == SellingReach.PUBLIC
                || (sameCity && product.getSellingReach() == SellingReach.OTHER_COLLEGES)
                || (sameCollege && product.getSellingReach() == SellingReach.MY_CAMPUS);
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
