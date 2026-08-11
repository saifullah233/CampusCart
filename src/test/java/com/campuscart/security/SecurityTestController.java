package com.campuscart.security;

import com.campuscart.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Test-only endpoints for exercising the real filter chain and method security. */
@Hidden
@RestController
@RequestMapping("/test/security")
public class SecurityTestController {

    @GetMapping("/protected")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PrincipalView> protectedEndpoint(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(new PrincipalView(user.id(), user.role().name()));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> adminEndpoint() {
        return ApiResponse.ok("admin");
    }

    @GetMapping("/resources/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> uuidEndpoint(@PathVariable UUID id) {
        return ApiResponse.ok(id.toString());
    }

    public record PrincipalView(UUID userId, String role) {
    }
}
