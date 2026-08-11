package com.campuscart.chat.web;

import com.campuscart.chat.dto.ChatReportResponse;
import com.campuscart.chat.dto.ReportUserRequest;
import com.campuscart.chat.dto.ReportProductRequest;
import com.campuscart.chat.service.ChatService;
import com.campuscart.common.api.ApiResponse;
import com.campuscart.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ChatService chatService;

    public ReportController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/users/{targetUserId}")
    public ResponseEntity<ApiResponse<ChatReportResponse>> reportUser(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID targetUserId,
            @Valid @RequestBody ReportUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Report submitted.",
                        chatService.reportUser(principal.id(), targetUserId, request)));
    }

    @PostMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<ChatReportResponse>> reportProduct(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID productId,
            @Valid @RequestBody ReportProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Report submitted.",
                        chatService.reportProduct(principal.id(), productId, request)));
    }
}
