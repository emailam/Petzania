package com.example.notificationmodule.service.impl;

import com.example.notificationmodule.model.dto.NotificationDTO;
import com.example.notificationmodule.service.IWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j

public class WebSocketService implements IWebSocketService {
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendNotificationToUser(UUID userId, NotificationDTO notificationDTO) {
        log.info("Sending real-time notification to user: {}", userId);

        try {
            // Send to specific user's notification channel
            String destination = "/user/" + userId + "/queue/notifications";
            messagingTemplate.convertAndSend(destination, notificationDTO);

            log.info("Notification sent successfully to destination: {}", destination);
        } catch(Exception e){
            log.error("Failed to send notification to user: {}", userId, e);
        }
    }

    @Override
    public void sendNotificationCountUpdate(UUID userId, long unreadCount) {
        log.info("Sending notification count update to user: {} - count: {}", userId, unreadCount);

        try {
            String destination = "/user/" + userId + "/queue/notification-count";
            messagingTemplate.convertAndSend(destination, unreadCount);

            log.info("Notification count sent successfully to destination: {}", destination);
        } catch(Exception e){
            log.error("Failed to send notification count to user: {}", userId, e);
        }
    }
}
