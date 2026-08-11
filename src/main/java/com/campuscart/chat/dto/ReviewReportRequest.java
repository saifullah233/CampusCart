package com.campuscart.chat.dto;

import com.campuscart.chat.domain.ChatReportStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewReportRequest(@NotNull ChatReportStatus status) {
}
