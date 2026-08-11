package com.campuscart.review.dto;

import com.campuscart.review.domain.ReviewStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewModerationRequest(@NotNull ReviewStatus status) {
}
