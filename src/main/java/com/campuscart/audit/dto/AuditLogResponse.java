package com.campuscart.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(UUID id, UUID actorId, String actorEmail, String action, String targetType,
                               UUID targetId, String details, Instant createdAt) {
}
