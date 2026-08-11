package com.campuscart.chat.web;

import com.campuscart.chat.dto.ChatMessageResponse;
import com.campuscart.chat.dto.ChatReportResponse;
import com.campuscart.chat.dto.ConversationResponse;
import com.campuscart.chat.dto.ReportMessageRequest;
import com.campuscart.chat.dto.SendMessageRequest;
import com.campuscart.chat.dto.ShareProductRequest;
import com.campuscart.chat.service.ChatService;
import com.campuscart.common.api.ApiResponse;
import com.campuscart.common.api.PageResponse;
import com.campuscart.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/conversations")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> start(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam UUID productId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Conversation ready.", chatService.startConversation(principal.id(), productId)));
    }

    @GetMapping
    public ApiResponse<PageResponse<ConversationResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(chatService.list(principal.id(), page, size));
    }

    @GetMapping("/{conversationId}")
    public ApiResponse<ConversationResponse> get(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @PathVariable UUID conversationId) {
        return ApiResponse.ok(chatService.get(principal.id(), conversationId));
    }

    @GetMapping("/{conversationId}/messages")
    public ApiResponse<PageResponse<ChatMessageResponse>> messages(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(chatService.messages(principal.id(), conversationId, page, size));
    }

    @PostMapping("/{conversationId}/messages")
    public ApiResponse<ChatMessageResponse> sendText(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.ok(chatService.sendText(principal.id(), conversationId, request.content()));
    }

    @PostMapping("/{conversationId}/messages/product")
    public ApiResponse<ChatMessageResponse> shareProduct(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody ShareProductRequest request) {
        return ApiResponse.ok(chatService.shareProduct(principal.id(), conversationId, request.productId()));
    }

    @PostMapping(value = "/{conversationId}/messages/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ChatMessageResponse> sendImage(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID conversationId,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(chatService.sendImage(principal.id(), conversationId, file));
    }

    @PostMapping("/{conversationId}/read")
    public ApiResponse<Long> markRead(@AuthenticationPrincipal AuthenticatedUser principal,
                                      @PathVariable UUID conversationId) {
        return ApiResponse.ok(chatService.markRead(principal.id(), conversationId));
    }

    @GetMapping("/{conversationId}/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal AuthenticatedUser principal,
                                         @PathVariable UUID conversationId) {
        return ApiResponse.ok(chatService.unreadCount(principal.id(), conversationId));
    }

    @PostMapping("/{conversationId}/report")
    public ResponseEntity<ApiResponse<ChatReportResponse>> report(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody ReportMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Report submitted.", chatService.report(principal.id(), conversationId, request)));
    }
}
