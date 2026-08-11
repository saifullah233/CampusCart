package com.campuscart.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.campuscart.chat.repository.ConversationRepository;
import com.campuscart.security.websocket.JwtStompChannelInterceptor;
import com.campuscart.user.domain.AccountStatus;
import com.campuscart.user.domain.Role;
import com.campuscart.user.domain.User;
import com.campuscart.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class WebSocketAuthenticationTest {

    @Test
    void connectReconstructsPrincipalFromVerifiedJwt() {
        JwtService jwtService = mock(JwtService.class);
        AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), "person@example.com",
                Role.STUDENT);
        when(jwtService.parseAccessToken("token")).thenReturn(principal);
        JwtStompChannelInterceptor interceptor = new JwtStompChannelInterceptor(jwtService,
                mock(ConversationRepository.class), mock(UserRepository.class));
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        headers.addNativeHeader("Authorization", "Bearer token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        Message<?> authenticated = interceptor.preSend(message, mock(MessageChannel.class));
        assertThat(StompHeaderAccessor.wrap(authenticated).getUser().getName())
                .isEqualTo("person@example.com");
    }

    @Test
    void missingOrInvalidConnectTokenIsRejected() {
        JwtService jwtService = mock(JwtService.class);
        when(jwtService.parseAccessToken("bad")).thenThrow(new com.campuscart.common.exception.InvalidTokenException());
        JwtStompChannelInterceptor interceptor = new JwtStompChannelInterceptor(jwtService,
                mock(ConversationRepository.class), mock(UserRepository.class));
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void participantCanSubscribeToConversationTopic() {
        JwtService jwtService = mock(JwtService.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), "person@example.com", Role.STUDENT);
        UUID conversationId = UUID.randomUUID();
        when(conversationRepository.findByIdAndParticipant(conversationId, principal.id()))
                .thenReturn(java.util.Optional.of(mock(com.campuscart.chat.domain.Conversation.class)));
        JwtStompChannelInterceptor interceptor = new JwtStompChannelInterceptor(jwtService, conversationRepository,
                mock(UserRepository.class));
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        headers.setDestination("/topic/conversations/" + conversationId);
        headers.setUser(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        assertThat(interceptor.preSend(message, mock(MessageChannel.class))).isNotNull();
    }

    @Test
    void nonParticipantCannotSubscribeToConversationTopic() {
        JwtService jwtService = mock(JwtService.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), "person@example.com", Role.STUDENT);
        UUID conversationId = UUID.randomUUID();
        when(conversationRepository.findByIdAndParticipant(conversationId, principal.id()))
                .thenReturn(java.util.Optional.empty());
        JwtStompChannelInterceptor interceptor = new JwtStompChannelInterceptor(jwtService, conversationRepository,
                mock(UserRepository.class));
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        headers.setDestination("/topic/conversations/" + conversationId);
        headers.setUser(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void clientCannotPublishDirectlyToConversationTopic() {
        JwtService jwtService = mock(JwtService.class);
        JwtStompChannelInterceptor interceptor = new JwtStompChannelInterceptor(jwtService,
                mock(ConversationRepository.class), mock(UserRepository.class));
        AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), "person@example.com", Role.STUDENT);
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination("/topic/conversations/" + UUID.randomUUID());
        headers.setUser(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void suspendedUserCannotEstablishStompConnectionWithExistingToken() {
        JwtService jwtService = mock(JwtService.class);
        UserRepository userRepository = mock(UserRepository.class);
        AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), "person@example.com",
                Role.STUDENT);
        User suspendedUser = mock(User.class);
        when(jwtService.parseAccessToken("token")).thenReturn(principal);
        when(userRepository.findById(principal.id())).thenReturn(java.util.Optional.of(suspendedUser));
        when(suspendedUser.getStatus()).thenReturn(AccountStatus.SUSPENDED);
        JwtStompChannelInterceptor interceptor = new JwtStompChannelInterceptor(jwtService,
                mock(ConversationRepository.class), userRepository);
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        headers.addNativeHeader("Authorization", "Bearer token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("account is not active");
    }
}
