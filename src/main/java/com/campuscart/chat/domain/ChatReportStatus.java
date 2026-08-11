package com.campuscart.chat.domain;

public enum ChatReportStatus {
    PENDING,
    UNDER_REVIEW,
    OPEN,
    REVIEWED,
    RESOLVED,
    DISMISSED;

    public boolean isActive() {
        return this == PENDING || this == UNDER_REVIEW || this == OPEN;
    }

    public boolean canTransitionTo(ChatReportStatus target) {
        return switch (this) {
            case PENDING, OPEN -> target == UNDER_REVIEW || target == RESOLVED || target == DISMISSED;
            case UNDER_REVIEW -> target == RESOLVED || target == DISMISSED;
            case REVIEWED, RESOLVED, DISMISSED -> false;
        };
    }

    public static java.util.Set<ChatReportStatus> activeStatuses() {
        return java.util.EnumSet.of(PENDING, UNDER_REVIEW, OPEN);
    }
}
