package com.campuscart.payment.web;

import com.campuscart.common.api.ApiResponse;
import com.campuscart.payment.dto.PaymentResponse;
import com.campuscart.payment.service.PaymentService;
import com.campuscart.security.AuthenticatedUser;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/orders/{orderId}/initialize")
    public ApiResponse<PaymentResponse> initialize(@AuthenticationPrincipal AuthenticatedUser principal,
                                                   @PathVariable UUID orderId) {
        return ApiResponse.ok(paymentService.initialize(principal.id(), orderId));
    }
}
