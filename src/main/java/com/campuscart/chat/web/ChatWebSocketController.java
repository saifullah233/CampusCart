package com.campuscart.chat.web;

import com.campuscart.chat.dto.ChatMessageResponse;
import com.campuscart.chat.dto.SendMessageRequest;
import com.campuscart.chat.dto.TypingEvent;
import com.campuscart.chat.service.ChatService;
import com.campuscart.security.AuthenticatedUser;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/conversations/{conversationId}/message")
    public void sendMessage(@AuthenticationPrincipal AuthenticatedUser principal,
                            @DestinationVariable UUID conversationId,
                            SendMessageRequest request) {
        chatService.sendText(principal.id(), conversationId, request.content());
    }

    @MessageMapping("/conversations/{conversationId}/typing")
    public void typing(@AuthenticationPrincipal AuthenticatedUser principal,
                       @DestinationVariable UUID conversationId,
                       TypingEvent event) {
        chatService.get(principal.id(), conversationId);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/typing",
                new TypingEvent(event.typing()));
    }
}
