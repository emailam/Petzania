package com.example.notificationmodule.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for these destinations
        config.enableSimpleBroker("/topic", "/queue", "/user");

        // Set application destination prefix
        config.setApplicationDestinationPrefixes("/app");
        /*
         * Messages sent from client to server will have /app prefix
         * Example: client sends to /app/markAsRead -> server handles it
         */

        // Set user destination prefix
        config.setUserDestinationPrefix("/user");
        /*
         * For user-specific messaging
         * /user/{userId}/queue/notifications -> sends to specific user
         */
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications")
                .setAllowedOrigins("*") // allow all origins (configure properly in production)
                .withSockJS();

        /*
         * - Clients connect to: ws://localhost:8083/ws/notifications
         * - SockJS provides fallback options (polling, etc.) if WebSocket fails
         * - In production, replace "*" with specific allowed origins
         */
    }
}
