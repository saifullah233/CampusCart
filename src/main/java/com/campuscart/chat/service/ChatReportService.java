package com.campuscart.chat.service;

import com.campuscart.chat.domain.ChatReport;
import com.campuscart.chat.domain.ChatReportStatus;
import com.campuscart.chat.dto.ChatReportResponse;
import com.campuscart.chat.dto.ReviewReportRequest;
import com.campuscart.audit.service.AuditLogService;
import com.campuscart.chat.repository.ChatReportRepository;
import com.campuscart.common.api.PageResponse;
import com.campuscart.common.exception.InvalidReportException;
import com.campuscart.common.exception.ResourceNotFoundException;
import com.campuscart.user.domain.User;
import com.campuscart.user.service.UserService;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatReportService {

    private final ChatReportRepository reportRepository;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public ChatReportService(ChatReportRepository reportRepository, UserService userService,
                             AuditLogService auditLogService, Clock clock) {
        this.reportRepository = reportRepository;
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatReportResponse> listOpen(UUID adminId, int page, int size) {
        requireAdmin(adminId);
        validatePage(page, size);
        return PageResponse.from(reportRepository.findByStatusInOrderByCreatedAtAsc(
                        ChatReportStatus.activeStatuses(),
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")))
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatReportResponse> list(UUID adminId, ChatReportStatus status, int page, int size) {
        requireAdmin(adminId);
        validatePage(page, size);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        if (status == null) {
            return PageResponse.from(reportRepository.findAll(pageable).map(this::toResponse));
        }
        return PageResponse.from(reportRepository.findByStatusOrderByCreatedAtAsc(status, pageable)
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatReportResponse> listReportedProducts(UUID adminId, int page, int size) {
        requireAdmin(adminId);
        validatePage(page, size);
        return PageResponse.from(reportRepository.findByReportedProductIsNotNullAndStatusInOrderByCreatedAtAsc(
                        ChatReportStatus.activeStatuses(),
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")))
                .map(this::toResponse));
    }

    @Transactional
    public ChatReportResponse review(UUID adminId, UUID reportId, ReviewReportRequest request) {
        User admin = requireAdmin(adminId);
        ChatReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> ResourceNotFoundException.of("Chat report", reportId));
        if (!report.getStatus().canTransitionTo(request.status())) {
            throw new InvalidReportException("The report status transition is not permitted.");
        }
        report.review(admin, request.status(), clock.instant());
        auditLogService.record(admin, "REPORT_STATUS_CHANGED", "REPORT", reportId,
                "Report status changed to " + request.status().name() + ".");
        return toResponse(report);
    }

    private User requireAdmin(UUID adminId) {
        User user = userService.requireActive(adminId);
        if (!user.getRole().isAdmin()) {
            throw new AccessDeniedException("Administrator access is required for moderation.");
        }
        return user;
    }

    private ChatReportResponse toResponse(ChatReport report) {
        return new ChatReportResponse(report.getId(), report.getReporter().getId(),
                report.getConversation() == null ? null : report.getConversation().getId(),
                report.getReportedUser() == null ? null : report.getReportedUser().getId(),
                report.getReportedProduct() == null ? null : report.getReportedProduct().getId(),
                report.getMessage() == null ? null : report.getMessage().getId(), report.getReason(), report.getDetails(),
                report.getStatus(), report.getReviewedBy() == null ? null : report.getReviewedBy().getId(),
                report.getReviewedAt(), report.getCreatedAt());
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 50.");
        }
    }
}
