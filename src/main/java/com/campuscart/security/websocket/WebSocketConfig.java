package com.campuscart.security.websocket;

import com.campuscart.chat.repository.ConversationRepository;
import com.campuscart.security.CorsProperties;
import com.campuscart.security.JwtService;
import com.campuscart.user.repository.UserRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final CorsProperties corsProperties;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public WebSocketConfig(JwtService jwtService, CorsProperties corsProperties,
                           ConversationRepository conversationRepository,
                           UserRepository userRepository) {
        this.jwtService = jwtService;
        this.corsProperties = corsProperties;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(corsProperties.getAllowedOrigins().toArray(String[]::new));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new JwtStompChannelInterceptor(jwtService, conversationRepository, userRepository));
    }
}
