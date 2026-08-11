package com.campuscart.product.web;

import com.campuscart.common.api.ApiResponse;
import com.campuscart.product.service.ProductLikeService;
import com.campuscart.security.AuthenticatedUser;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products/{productId}/like")
public class ProductLikeController {

    private final ProductLikeService likeService;

    public ProductLikeController(ProductLikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping
    public ApiResponse<Boolean> like(@AuthenticationPrincipal AuthenticatedUser principal,
                                     @PathVariable UUID productId) {
        return ApiResponse.ok("Product liked.", likeService.like(principal.id(), productId));
    }

    @DeleteMapping
    public ApiResponse<Void> unlike(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @PathVariable UUID productId) {
        likeService.unlike(principal.id(), productId);
        return ApiResponse.ok("Product unliked.", null);
    }

    @GetMapping
    public ApiResponse<Boolean> liked(@AuthenticationPrincipal AuthenticatedUser principal,
                                      @PathVariable UUID productId) {
        return ApiResponse.ok(likeService.liked(principal.id(), productId));
    }
}
