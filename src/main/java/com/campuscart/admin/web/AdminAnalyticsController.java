package com.campuscart.admin.web;

import com.campuscart.admin.dto.AdminAnalyticsResponse;
import com.campuscart.admin.service.AdminAnalyticsService;
import com.campuscart.common.api.ApiResponse;
import com.campuscart.security.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;

    public AdminAnalyticsController(AdminAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminAnalyticsResponse> dashboard(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(analyticsService.dashboard(principal.id()));
    }

    @GetMapping("/analytics")
    public ApiResponse<AdminAnalyticsResponse> analytics(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(analyticsService.dashboard(principal.id()));
    }
}
