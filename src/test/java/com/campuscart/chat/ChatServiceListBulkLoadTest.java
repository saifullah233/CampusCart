package com.campuscart.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campuscart.chat.domain.Conversation;
import com.campuscart.chat.image.ChatImageStorage;
import com.campuscart.chat.repository.ChatMessageRepository;
import com.campuscart.chat.repository.ChatReportRepository;
import com.campuscart.chat.repository.ConversationRepository;
import com.campuscart.chat.safety.ChatContentSafetyService;
import com.campuscart.chat.safety.ChatImageSafetyScanner;
import com.campuscart.chat.service.BlockService;
import com.campuscart.chat.service.ChatService;
import com.campuscart.notification.service.NotificationService;
import com.campuscart.product.image.ImageFileValidator;
import com.campuscart.product.service.ProductService;
import com.campuscart.user.service.UserService;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Regression guard for the conversation-list N+1 fix: a page of conversations must
 * resolve unread counts from a single batched query, never one count query per row.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceListBulkLoadTest {

    @Mock ConversationRepository conversationRepository;
    @Mock ChatMessageRepository messageRepository;
    @Mock ChatReportRepository reportRepository;
    @Mock ProductService productService;
    @Mock UserService userService;
    @Mock BlockService blockService;
    @Mock ChatContentSafetyService contentSafetyService;
    @Mock ChatImageStorage imageStorage;
    @Mock ImageFileValidator imageFileValidator;
    @Mock ChatImageSafetyScanner imageSafetyScanner;
    @Mock NotificationService notificationService;
    @Mock SimpMessagingTemplate messagingTemplate;

    ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(conversationRepository, messageRepository, reportRepository,
                productService, userService, blockService, contentSafetyService, imageStorage,
                imageFileValidator, imageSafetyScanner, notificationService, messagingTemplate,
                Clock.systemUTC());
    }

    @Test
    void list_batchesUnreadCounts_withoutPerConversationCountQueries() {
        UUID userId = UUID.randomUUID();
        UUID firstConversationId = UUID.randomUUID();
        UUID secondConversationId = UUID.randomUUID();

        Conversation first = mock(Conversation.class, RETURNS_DEEP_STUBS);
        when(first.getId()).thenReturn(firstConversationId);
        Conversation second = mock(Conversation.class, RETURNS_DEEP_STUBS);
        when(second.getId()).thenReturn(secondConversationId);
        var page = new PageImpl<>(List.of(first, second), PageRequest.of(0, 20), 2);
        when(conversationRepository.findByParticipant(eq(userId), any())).thenReturn(page);

        ChatMessageRepository.ConversationUnreadCount unread =
                mock(ChatMessageRepository.ConversationUnreadCount.class);
        when(unread.getConversationId()).thenReturn(firstConversationId);
        when(unread.getUnread()).thenReturn(4L);
        when(messageRepository.countUnreadForRecipientByConversationIds(
                List.of(firstConversationId, secondConversationId), userId)).thenReturn(List.of(unread));

        chatService.list(userId, 0, 20);

        // One batched count query for the whole page.
        verify(messageRepository).countUnreadForRecipientByConversationIds(
                List.of(firstConversationId, secondConversationId), userId);
        // The per-conversation (N+1) count must not be used on the list path.
        verify(messageRepository, never()).countUnreadForRecipient(any(), any());
    }
}
