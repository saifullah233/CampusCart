package com.campuscart.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID buyerId,
        String buyerName,
        UUID sellerId,
        String sellerName,
        UUID productId,
        String productTitle,
        Instant lastMessageAt,
        long unreadCount) {
}
