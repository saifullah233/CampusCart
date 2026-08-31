package com.campuscart.chat.service;

import com.campuscart.chat.domain.ChatMessage;
import com.campuscart.chat.domain.ChatMessageType;
import com.campuscart.chat.domain.ChatModerationStatus;
import com.campuscart.chat.domain.ChatReport;
import com.campuscart.chat.domain.ChatReportStatus;
import com.campuscart.chat.domain.Conversation;
import com.campuscart.chat.dto.ChatMessageResponse;
import com.campuscart.chat.dto.ChatReportResponse;
import com.campuscart.chat.dto.ConversationResponse;
import com.campuscart.chat.dto.ReportMessageRequest;
import com.campuscart.chat.dto.ReportUserRequest;
import com.campuscart.chat.dto.ReportProductRequest;
import com.campuscart.chat.image.ChatImageStorage;
import com.campuscart.chat.repository.ChatMessageRepository;
import com.campuscart.chat.repository.ChatReportRepository;
import com.campuscart.chat.repository.ConversationRepository;
import com.campuscart.chat.safety.ChatContentSafetyService;
import com.campuscart.chat.safety.ChatImageSafetyScanner;
import com.campuscart.common.api.PageResponse;
import com.campuscart.common.exception.DuplicateResourceException;
import com.campuscart.common.exception.InvalidRequestException;
import com.campuscart.common.exception.InvalidReportException;
import com.campuscart.common.exception.ProductUnavailableException;
import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.common.exception.UserBlockedException;
import com.campuscart.notification.domain.NotificationType;
import com.campuscart.notification.service.NotificationService;
import com.campuscart.product.domain.Product;
import com.campuscart.product.image.ImageFileValidator;
import com.campuscart.product.service.ProductService;
import com.campuscart.security.AuthenticatedUser;
import com.campuscart.user.domain.User;
import com.campuscart.user.service.UserService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatReportRepository reportRepository;
    private final ProductService productService;
    private final UserService userService;
    private final BlockService blockService;
    private final ChatContentSafetyService contentSafetyService;
    private final ChatImageStorage imageStorage;
    private final ImageFileValidator imageFileValidator;
    private final ChatImageSafetyScanner imageSafetyScanner;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    public ChatService(ConversationRepository conversationRepository,
                       ChatMessageRepository messageRepository,
                       ChatReportRepository reportRepository,
                       ProductService productService,
                       UserService userService,
                       BlockService blockService,
                       ChatContentSafetyService contentSafetyService,
                       ChatImageStorage imageStorage,
                       ImageFileValidator imageFileValidator,
                       ChatImageSafetyScanner imageSafetyScanner,
                       NotificationService notificationService,
                       SimpMessagingTemplate messagingTemplate,
                       Clock clock) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.reportRepository = reportRepository;
        this.productService = productService;
        this.userService = userService;
        this.blockService = blockService;
        this.contentSafetyService = contentSafetyService;
        this.imageStorage = imageStorage;
        this.imageFileValidator = imageFileValidator;
        this.imageSafetyScanner = imageSafetyScanner;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
        this.clock = clock;
    }

    @Transactional
    public ConversationResponse startConversation(UUID buyerId, UUID productId) {
        User buyer = userService.requireActive(buyerId);
        Product product = productService.requireDiscoverable(buyerId, productId);
        if (product.getSeller().getId().equals(buyerId)) {
            throw new ProductUnavailableException("You cannot start a buyer conversation with your own product.");
        }
        ensureNotBlocked(buyerId, product.getSeller().getId());
        Conversation conversation = conversationRepository
                .findByBuyerIdAndSellerIdAndProductId(buyerId, product.getSeller().getId(), productId)
                .orElseGet(() -> conversationRepository.save(new Conversation(buyer, product.getSeller(), product)));
        return toConversationResponse(conversation, buyerId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> list(UUID userId, int page, int size) {
        userService.requireActive(userId);
        validatePage(page, size);
        var conversations = conversationRepository.findByParticipant(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
        // Batch the unread counts for the whole page instead of one count query per row.
        List<UUID> conversationIds = conversations.getContent().stream().map(Conversation::getId).toList();
        Map<UUID, Long> unreadByConversation = conversationIds.isEmpty() ? Map.of()
                : messageRepository.countUnreadForRecipientByConversationIds(conversationIds, userId).stream()
                        .collect(Collectors.toMap(
                                ChatMessageRepository.ConversationUnreadCount::getConversationId,
                                ChatMessageRepository.ConversationUnreadCount::getUnread));
        return PageResponse.from(conversations.map(conversation -> toConversationResponse(conversation,
                unreadByConversation.getOrDefault(conversation.getId(), 0L))));
    }

    @Transactional(readOnly = true)
    public ConversationResponse get(UUID userId, UUID conversationId) {
        return toConversationResponse(requireParticipant(userId, conversationId), userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatMessageResponse> messages(UUID userId, UUID conversationId, int page, int size) {
        requireParticipant(userId, conversationId);
        validatePage(page, size);
        return PageResponse.from(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")))
                .map(this::toMessageResponse));
    }

    @Transactional
    public ChatMessageResponse sendText(UUID senderId, UUID conversationId, String content) {
        Conversation conversation = requireParticipant(senderId, conversationId);
        ensureNotBlocked(conversation, senderId);
        contentSafetyService.validateText(content);
        ChatMessage message = ChatMessage.text(conversation, userService.requireActive(senderId), content.trim());
        return persistMessage(conversation, message, senderId);
    }

    @Transactional
    public ChatMessageResponse shareProduct(UUID senderId, UUID conversationId, UUID productId) {
        Conversation conversation = requireParticipant(senderId, conversationId);
        ensureNotBlocked(conversation, senderId);
        Product product = productService.requireDiscoverable(senderId, productId);
        ChatMessage message = ChatMessage.product(conversation, userService.requireActive(senderId), product);
        return persistMessage(conversation, message, senderId);
    }

    @Transactional
    public ChatMessageResponse sendImage(UUID senderId, UUID conversationId, MultipartFile file) {
        Conversation conversation = requireParticipant(senderId, conversationId);
        ensureNotBlocked(conversation, senderId);
        ImageFileValidator.ValidatedImage validated = imageFileValidator.validate(file);
        ChatImageSafetyScanner.ImageSafetyDecision safety = imageSafetyScanner.scan(file);
        ChatImageStorage.StoredImage stored = imageStorage.store(conversationId, file);
        ChatModerationStatus moderationStatus = safety == ChatImageSafetyScanner.ImageSafetyDecision.CLEAR
                ? ChatModerationStatus.CLEAR : ChatModerationStatus.PENDING_REVIEW;
        try {
            ChatMessage message = ChatMessage.image(conversation, userService.requireActive(senderId),
                    stored.storageKey(), stored.deliveryUrl(), validated.contentType(), validated.sizeBytes(),
                    moderationStatus);
            return persistMessage(conversation, message, senderId);
        } catch (RuntimeException ex) {
            imageStorage.delete(stored.storageKey());
            throw ex;
        }
    }

    @Transactional
    public long markRead(UUID userId, UUID conversationId) {
        requireParticipant(userId, conversationId);
        var unread = messageRepository.findUnreadForRecipient(conversationId, userId);
        Instant now = clock.instant();
        unread.forEach(message -> message.markRead(now));
        return unread.size();
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId, UUID conversationId) {
        requireParticipant(userId, conversationId);
        return messageRepository.countUnreadForRecipient(conversationId, userId);
    }

    @Transactional
    public ChatReportResponse report(UUID reporterId, UUID conversationId, ReportMessageRequest request) {
        Conversation conversation = requireParticipant(reporterId, conversationId);
        ChatMessage message = null;
        if (request.messageId() != null) {
            message = messageRepository.findById(request.messageId())
                    .orElseThrow(() -> new InvalidReportException("Reported message was not found."));
            if (!message.getConversation().getId().equals(conversationId)) {
                throw new InvalidReportException("Reported message does not belong to this conversation.");
            }
        }
        if (reportRepository.existsByReporterIdAndConversationIdAndMessageIdAndStatusIn(
                reporterId, conversationId, request.messageId(), ChatReportStatus.activeStatuses())) {
            throw new DuplicateResourceException("An active report for this target already exists.");
        }
        ChatReport report = reportRepository.save(new ChatReport(userService.requireActive(reporterId),
                conversation, message, request.reason().trim(), request.details()));
        return toReportResponse(report);
    }

    @Transactional
    public ChatReportResponse reportUser(UUID reporterId, UUID targetUserId, ReportUserRequest request) {
        User reporter = userService.requireActive(reporterId);
        User target = userService.requireActive(targetUserId);
        if (reporterId.equals(targetUserId)) {
            throw new InvalidReportException("You cannot report your own account.");
        }
        if (reportRepository.existsByReporterIdAndReportedUserIdAndStatusIn(
                reporterId, targetUserId, ChatReportStatus.activeStatuses())) {
            throw new DuplicateResourceException("An active report for this user already exists.");
        }
        ChatReport report = reportRepository.save(new ChatReport(reporter, target,
                request.reason().trim(), request.details()));
        return toReportResponse(report);
    }

    @Transactional
    public ChatReportResponse reportProduct(UUID reporterId, UUID productId, ReportProductRequest request) {
        User reporter = userService.requireActive(reporterId);
        Product product = productService.requireDiscoverable(reporterId, productId);
        if (product.getSeller().getId().equals(reporterId)) {
            throw new InvalidReportException("You cannot report your own product.");
        }
        if (reportRepository.existsByReporterIdAndReportedProductIdAndStatusIn(
                reporterId, productId, ChatReportStatus.activeStatuses())) {
            throw new DuplicateResourceException("An active report for this product already exists.");
        }
        ChatReport report = reportRepository.save(new ChatReport(reporter, product,
                request.reason().trim(), request.details()));
        return toReportResponse(report);
    }

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    private ChatMessageResponse persistMessage(Conversation conversation, ChatMessage message, UUID senderId) {
        ChatMessage saved = messageRepository.save(message);
        Instant sentAt = saved.getCreatedAt() == null ? clock.instant() : saved.getCreatedAt();
        conversation.recordMessage(sentAt);
        ChatMessageResponse response = toMessageResponse(saved);
        publishAfterCommit(conversation.getId(), response);
        UUID recipientId = conversation.getBuyer().getId().equals(senderId)
                ? conversation.getSeller().getId() : conversation.getBuyer().getId();
        User sender = saved.getSender();
        String senderName = sender != null && sender.getFullName() != null ? sender.getFullName() : "CampusCart Member";
        String messageText = saved.getContent() != null && !saved.getContent().isBlank()
                ? saved.getContent() : "You have a new message.";
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("conversationId", conversation.getId().toString());
        data.put("messageId", saved.getId().toString());
        data.put("senderId", senderId.toString());
        data.put("senderName", senderName);
        data.put("messageContent", messageText);
        if (conversation.getProduct() != null) {
            data.put("productId", conversation.getProduct().getId().toString());
            data.put("productTitle", conversation.getProduct().getTitle());
            data.put("price", conversation.getProduct().getPrice());
        }
        String dataJson;
        try {
            dataJson = objectMapper.writeValueAsString(data);
        } catch (Exception ex) {
            dataJson = "{\"conversationId\":\"" + conversation.getId() + "\",\"messageId\":\"" + saved.getId() + "\"}";
        }
        notificationService.create(recipientId, NotificationType.NEW_MESSAGE,
                "New message from " + senderName, messageText, dataJson);
        return response;
    }

    private Conversation requireParticipant(UUID userId, UUID conversationId) {
        userService.requireActive(userId);
        return conversationRepository.findByIdAndParticipant(conversationId, userId)
                .orElseThrow(() -> new AccessDeniedException("Conversation access is restricted to its participants."));
    }

    private void ensureNotBlocked(Conversation conversation, UUID senderId) {
        UUID other = conversation.getBuyer().getId().equals(senderId)
                ? conversation.getSeller().getId() : conversation.getBuyer().getId();
        ensureNotBlocked(senderId, other);
    }

    private void ensureNotBlocked(UUID firstUserId, UUID secondUserId) {
        if (blockService.blockedBetween(firstUserId, secondUserId)) {
            throw new UserBlockedException();
        }
    }

    private ConversationResponse toConversationResponse(Conversation conversation, UUID viewerId) {
        return toConversationResponse(conversation,
                messageRepository.countUnreadForRecipient(conversation.getId(), viewerId));
    }

    private ConversationResponse toConversationResponse(Conversation conversation, long unreadCount) {
        return new ConversationResponse(conversation.getId(), conversation.getBuyer().getId(),
                conversation.getBuyer().getFullName(), conversation.getSeller().getId(),
                conversation.getSeller().getFullName(), conversation.getProduct().getId(),
                conversation.getProduct().getTitle(), conversation.getLastMessageAt(),
                unreadCount);
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        boolean imageVisible = message.getModerationStatus() == ChatModerationStatus.CLEAR;
        return new ChatMessageResponse(message.getId(), message.getConversation().getId(),
                message.getSender().getId(), message.getSender().getFullName(), message.getMessageType(),
                message.getContent(), imageVisible ? message.getImageDeliveryUrl() : null,
                message.getImageContentType(), message.getImageSizeBytes(),
                message.getSharedProduct() == null ? null : message.getSharedProduct().getId(),
                message.getModerationStatus(), message.getReadAt(), message.getCreatedAt());
    }

    private ChatReportResponse toReportResponse(ChatReport report) {
        return new ChatReportResponse(report.getId(), report.getReporter().getId(),
                report.getConversation() == null ? null : report.getConversation().getId(),
                report.getReportedUser() == null ? null : report.getReportedUser().getId(),
                report.getReportedProduct() == null ? null : report.getReportedProduct().getId(),
                report.getMessage() == null ? null : report.getMessage().getId(),
                report.getReason(), report.getDetails(), report.getStatus(),
                report.getReviewedBy() == null ? null : report.getReviewedBy().getId(),
                report.getReviewedAt(), report.getCreatedAt());
    }

    private void publishAfterCommit(UUID conversationId, ChatMessageResponse response) {
        Runnable publish = () -> messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);
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

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new InvalidRequestException("Page must be non-negative and size must be between 1 and 50.");
        }
    }
}
