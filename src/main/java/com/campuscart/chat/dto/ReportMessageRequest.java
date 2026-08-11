package com.campuscart.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ReportMessageRequest(
        @NotBlank @Size(max = 80) String reason,
        @Size(max = 1000) String details,
        UUID messageId) {
}
