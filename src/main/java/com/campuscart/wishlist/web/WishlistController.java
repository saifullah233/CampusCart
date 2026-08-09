package com.campuscart.wishlist.web;

import com.campuscart.common.api.ApiResponse;
import com.campuscart.common.api.PageResponse;
import com.campuscart.security.AuthenticatedUser;
import com.campuscart.wishlist.dto.WishlistItemResponse;
import com.campuscart.wishlist.service.WishlistService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<WishlistItemResponse>> add(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID productId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product added to wishlist.", wishlistService.add(principal.id(), productId)));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> remove(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @PathVariable UUID productId) {
        wishlistService.remove(principal.id(), productId);
        return ApiResponse.ok("Product removed from wishlist.", null);
    }

    @GetMapping
    public ApiResponse<PageResponse<WishlistItemResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(wishlistService.list(principal.id(), page, size));
    }

    @GetMapping("/check/{productId}")
    public ApiResponse<Boolean> contains(@AuthenticationPrincipal AuthenticatedUser principal,
                                         @PathVariable UUID productId) {
        return ApiResponse.ok(wishlistService.contains(principal.id(), productId));
    }
}
