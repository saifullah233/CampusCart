package com.campuscart.cart.web;

import com.campuscart.cart.dto.AddCartItemRequest;
import com.campuscart.cart.dto.CartItemResponse;
import com.campuscart.cart.dto.CartResponse;
import com.campuscart.cart.dto.UpdateCartItemRequest;
import com.campuscart.cart.service.CartService;
import com.campuscart.common.api.ApiResponse;
import com.campuscart.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartItemResponse>> add(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product added to cart.", cartService.add(principal.id(), request)));
    }

    @PatchMapping("/items/{productId}")
    public ApiResponse<CartItemResponse> update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.ok(cartService.update(principal.id(), productId, request));
    }

    @DeleteMapping("/items/{productId}")
    public ApiResponse<Void> remove(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @PathVariable UUID productId) {
        cartService.remove(principal.id(), productId);
        return ApiResponse.ok("Product removed from cart.", null);
    }

    @GetMapping
    public ApiResponse<CartResponse> get(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(cartService.get(principal.id()));
    }
}
