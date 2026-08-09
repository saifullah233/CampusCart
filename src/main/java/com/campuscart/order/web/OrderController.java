package com.campuscart.order.web;

import com.campuscart.common.api.ApiResponse;
import com.campuscart.common.api.PageResponse;
import com.campuscart.order.domain.OrderStatus;
import com.campuscart.order.dto.OrderResponse;
import com.campuscart.order.dto.UpdateOrderStatusRequest;
import com.campuscart.order.service.OrderService;
import com.campuscart.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order placed.", orderService.createFromCart(principal.id())));
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> buyerHistory(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(orderService.buyerHistory(principal.id(), page, size));
    }

    @GetMapping("/seller")
    public ApiResponse<PageResponse<OrderResponse>> sellerHistory(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(orderService.sellerHistory(principal.id(), page, size));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> get(@AuthenticationPrincipal AuthenticatedUser principal,
                                          @PathVariable UUID orderId) {
        return ApiResponse.ok(orderService.get(principal.id(), orderId));
    }

    @PatchMapping("/{orderId}/status")
    public ApiResponse<OrderResponse> status(@AuthenticationPrincipal AuthenticatedUser principal,
                                              @PathVariable UUID orderId,
                                              @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ApiResponse.ok(orderService.transition(principal.id(), orderId, request.status()));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancel(@AuthenticationPrincipal AuthenticatedUser principal,
                                             @PathVariable UUID orderId) {
        return ApiResponse.ok(orderService.transition(principal.id(), orderId, OrderStatus.CANCELLED));
    }

    @PostMapping("/{orderId}/complete")
    public ApiResponse<OrderResponse> complete(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @PathVariable UUID orderId) {
        return ApiResponse.ok(orderService.transition(principal.id(), orderId, OrderStatus.COMPLETED));
    }
}
