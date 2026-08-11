package com.campuscart.security.websocket;

import com.campuscart.chat.repository.ConversationRepository;
import com.campuscart.security.AuthenticatedUser;
import com.campuscart.security.JwtService;
import com.campuscart.user.repository.UserRepository;
import java.util.UUID;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import java.security.Principal;

/** Authenticates STOMP CONNECT and refuses any unauthenticated application frame. */
public final class JwtStompChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CONVERSATION_TOPIC_PREFIX = "/topic/conversations/";

    private final JwtService jwtService;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public JwtStompChannelInterceptor(JwtService jwtService,
                                      ConversationRepository conversationRepository,
                                      UserRepository userRepository) {
        this.jwtService = jwtService;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION)));
        } else if (command == StompCommand.SEND || command == StompCommand.SUBSCRIBE
                || command == StompCommand.UNSUBSCRIBE || command == StompCommand.DISCONNECT) {
            if (accessor.getUser() == null) {
                throw new MessageDeliveryException("WebSocket authentication is required.");
            }
            if (command == StompCommand.SEND) {
                rejectClientBrokerSend(accessor.getDestination());
            } else if (command == StompCommand.SUBSCRIBE) {
                authorizeConversationSubscription(accessor);
            }
        }
        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }

    private void rejectClientBrokerSend(String destination) {
        if (destination != null && (destination.startsWith("/topic/") || destination.startsWith("/queue/")
                || destination.startsWith("/user/"))) {
            throw new MessageDeliveryException("Client messages must use an application destination.");
        }
    }

    private void authorizeConversationSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(CONVERSATION_TOPIC_PREFIX)) {
            return;
        }

        UUID conversationId = parseConversationId(destination);
        AuthenticatedUser principal = authenticatedPrincipal(accessor.getUser());
        if (conversationRepository.findByIdAndParticipant(conversationId, principal.id()).isEmpty()) {
            throw new MessageDeliveryException("Conversation subscription is not permitted.");
        }
    }

    private UUID parseConversationId(String destination) {
        String remainder = destination.substring(CONVERSATION_TOPIC_PREFIX.length());
        int separator = remainder.indexOf('/');
        String conversationId = separator < 0 ? remainder : remainder.substring(0, separator);
        if (conversationId.isBlank()) {
            throw new MessageDeliveryException("Conversation subscription is not permitted.");
        }
        try {
            return UUID.fromString(conversationId);
        } catch (IllegalArgumentException ex) {
            throw new MessageDeliveryException("Conversation subscription is not permitted.");
        }
    }

    private AuthenticatedUser authenticatedPrincipal(Principal user) {
        if (!(user instanceof Authentication authentication)
                || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new MessageDeliveryException("WebSocket authentication is required.");
        }
        return principal;
    }

    private UsernamePasswordAuthenticationToken authenticate(String header) {
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new MessageDeliveryException("WebSocket authentication is required.");
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new MessageDeliveryException("WebSocket authentication is required.");
        }
        try {
            AuthenticatedUser principal = Objects.requireNonNull(jwtService.parseAccessToken(token));
            principal = userRepository.findById(principal.id())
                    .map(user -> {
                        if (!user.getStatus().canAuthenticate()) {
                            throw new MessageDeliveryException("The account is not active.");
                        }
                        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole());
                    })
                    .orElse(principal);
            return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        } catch (RuntimeException ex) {
            if (ex instanceof MessageDeliveryException) {
                throw ex;
            }
            throw new MessageDeliveryException("WebSocket authentication failed.");
        }
    }
}
