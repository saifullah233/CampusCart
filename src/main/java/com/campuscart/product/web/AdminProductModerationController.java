package com.campuscart.product.web;

import com.campuscart.chat.dto.ChatReportResponse;
import com.campuscart.common.api.ApiResponse;
import com.campuscart.common.api.PageResponse;
import com.campuscart.product.domain.ProductStatus;
import com.campuscart.product.dto.AdminProductResponse;
import com.campuscart.product.service.AdminProductModerationService;
import com.campuscart.security.AuthenticatedUser;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductModerationController {

    private final AdminProductModerationService productService;

    public AdminProductModerationController(AdminProductModerationService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminProductResponse>> list(@AuthenticationPrincipal AuthenticatedUser principal,
                                                                 @RequestParam(required = false) ProductStatus status,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(productService.list(principal.id(), status, page, size));
    }

    @GetMapping("/reported")
    public ApiResponse<PageResponse<ChatReportResponse>> reported(@AuthenticationPrincipal AuthenticatedUser principal,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(productService.reported(principal.id(), page, size));
    }

    @GetMapping("/{productId}")
    public ApiResponse<AdminProductResponse> get(@AuthenticationPrincipal AuthenticatedUser principal,
                                                  @PathVariable UUID productId) {
        return ApiResponse.ok(productService.get(principal.id(), productId));
    }

    @PostMapping("/{productId}/hide")
    public ApiResponse<AdminProductResponse> hide(@AuthenticationPrincipal AuthenticatedUser principal,
                                                   @PathVariable UUID productId) {
        return ApiResponse.ok(productService.hide(principal.id(), productId));
    }

    @PostMapping("/{productId}/restore")
    public ApiResponse<AdminProductResponse> restore(@AuthenticationPrincipal AuthenticatedUser principal,
                                                      @PathVariable UUID productId) {
        return ApiResponse.ok(productService.restore(principal.id(), productId));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<AdminProductResponse> remove(@AuthenticationPrincipal AuthenticatedUser principal,
                                                     @PathVariable UUID productId) {
        return ApiResponse.ok(productService.remove(principal.id(), productId));
    }
}
