package com.campuscart.chat.dto;

import com.campuscart.chat.domain.ChatReportStatus;
import java.time.Instant;
import java.util.UUID;

public record ChatReportResponse(
        UUID id,
        UUID reporterId,
        UUID conversationId,
        UUID reportedUserId,
        UUID reportedProductId,
        UUID messageId,
        String reason,
        String details,
        ChatReportStatus status,
        UUID reviewedBy,
        Instant reviewedAt,
        Instant createdAt) {
}
