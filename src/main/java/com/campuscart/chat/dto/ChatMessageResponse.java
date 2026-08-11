package com.campuscart.chat.dto;

import com.campuscart.chat.domain.ChatMessageType;
import com.campuscart.chat.domain.ChatModerationStatus;
import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderName,
        ChatMessageType messageType,
        String content,
        String imageUrl,
        String imageContentType,
        Long imageSizeBytes,
        UUID sharedProductId,
        ChatModerationStatus moderationStatus,
        Instant readAt,
        Instant createdAt) {
}
