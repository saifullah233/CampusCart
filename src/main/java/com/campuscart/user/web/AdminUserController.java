package com.campuscart.user.web;

import com.campuscart.common.api.ApiResponse;
import com.campuscart.common.api.PageResponse;
import com.campuscart.security.AuthenticatedUser;
import com.campuscart.user.domain.AccountStatus;
import com.campuscart.user.dto.AdminUserResponse;
import com.campuscart.user.service.AdminUserService;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> list(@AuthenticationPrincipal AuthenticatedUser principal,
                                                               @RequestParam(required = false) String query,
                                                               @RequestParam(required = false) AccountStatus status,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminUserService.list(principal.id(), query, status, page, size));
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserResponse> get(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @PathVariable UUID userId) {
        return ApiResponse.ok(adminUserService.get(principal.id(), userId));
    }

    @PostMapping("/{userId}/suspend")
    public ApiResponse<AdminUserResponse> suspend(@AuthenticationPrincipal AuthenticatedUser principal,
                                                   @PathVariable UUID userId) {
        return ApiResponse.ok(adminUserService.suspend(principal.id(), userId));
    }

    @PostMapping("/{userId}/activate")
    public ApiResponse<AdminUserResponse> activate(@AuthenticationPrincipal AuthenticatedUser principal,
                                                    @PathVariable UUID userId) {
        return ApiResponse.ok(adminUserService.activate(principal.id(), userId));
    }
}
