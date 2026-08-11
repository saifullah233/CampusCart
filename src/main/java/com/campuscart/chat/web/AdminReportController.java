package com.campuscart.chat.web;

import com.campuscart.chat.domain.ChatReportStatus;
import com.campuscart.chat.dto.ChatReportResponse;
import com.campuscart.chat.dto.ReviewReportRequest;
import com.campuscart.chat.service.ChatReportService;
import com.campuscart.common.api.ApiResponse;
import com.campuscart.common.api.PageResponse;
import com.campuscart.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final ChatReportService reportService;

    public AdminReportController(ChatReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ChatReportResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) ChatReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reportService.list(principal.id(), status, page, size));
    }

    @PatchMapping("/{reportId}")
    public ApiResponse<ChatReportResponse> review(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID reportId,
            @Valid @RequestBody ReviewReportRequest request) {
        return ApiResponse.ok(reportService.review(principal.id(), reportId, request));
    }
}
