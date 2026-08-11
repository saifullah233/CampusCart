package com.campuscart.notification.dto;

import com.campuscart.notification.domain.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String content,
        String dataJson,
        boolean read,
        Instant createdAt,
        Instant readAt) {
}
