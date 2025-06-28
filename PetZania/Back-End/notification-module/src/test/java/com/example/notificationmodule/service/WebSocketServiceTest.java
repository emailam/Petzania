package com.example.notificationmodule.service;

import com.example.notificationmodule.model.dto.NotificationDTO;
import com.example.notificationmodule.model.enumeration.NotificationStatus;
import com.example.notificationmodule.model.enumeration.NotificationType;
import com.example.notificationmodule.service.impl.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketService webSocketService;

    private UUID userId;
    private NotificationDTO notificationDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationDTO = NotificationDTO.builder()
                .notificationId(UUID.randomUUID())
                .recipientId(userId)
                .type(NotificationType.FRIEND_REQUEST_ACCEPTED)
                .message("Test notification")
                .status(NotificationStatus.UNREAD)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should send notification to user successfully")
    void shouldSendNotificationToUser() {
        // Act
        webSocketService.sendNotificationToUser(userId, notificationDTO);

        // Assert
        String expectedDestination = "/user/" + userId + "/queue/notifications";
        verify(messagingTemplate).convertAndSend(eq(expectedDestination), eq(notificationDTO));
    }

    @Test
    @DisplayName("Should handle exception when sending notification fails")
    void shouldHandleExceptionWhenSendingNotificationFails() {
        // Arrange
        String destination = "/user/" + userId + "/queue/notifications";
        doThrow(new RuntimeException("Connection failed"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // Act - Should not throw exception
        webSocketService.sendNotificationToUser(userId, notificationDTO);

        // Assert
        verify(messagingTemplate).convertAndSend(eq(destination), eq(notificationDTO));
    }

    @Test
    @DisplayName("Should send notification count update successfully")
    void shouldSendNotificationCountUpdate() {
        // Arrange
        long unreadCount = 5L;

        // Act
        webSocketService.sendNotificationCountUpdate(userId, unreadCount);

        // Assert
        String expectedDestination = "/user/" + userId + "/queue/notification-count";
        verify(messagingTemplate).convertAndSend(eq(expectedDestination), eq(unreadCount));
    }

    @Test
    @DisplayName("Should handle exception when sending count update fails")
    void shouldHandleExceptionWhenSendingCountUpdateFails() {
        // Arrange
        long unreadCount = 3L;
        String destination = "/user/" + userId + "/queue/notification-count";
        doThrow(new RuntimeException("Connection failed"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // Act - Should not throw exception
        webSocketService.sendNotificationCountUpdate(userId, unreadCount);

        // Assert
        verify(messagingTemplate).convertAndSend(eq(destination), eq(unreadCount));
    }

    @Test
    @DisplayName("Should handle null userId gracefully")
    void shouldHandleNullUserId() {
        // Act
        webSocketService.sendNotificationToUser(null, notificationDTO);
        webSocketService.sendNotificationCountUpdate(null, 0L);

        // Assert
        verify(messagingTemplate).convertAndSend(eq("/user/null/queue/notifications"), eq(notificationDTO));
        verify(messagingTemplate).convertAndSend(eq("/user/null/queue/notification-count"), eq(0L));
    }

    @Test
    @DisplayName("Should format destination correctly for different user IDs")
    void shouldFormatDestinationCorrectly() {
        // Arrange
        UUID customUserId = UUID.fromString("12345678-1234-1234-1234-123456789012");

        // Act
        webSocketService.sendNotificationToUser(customUserId, notificationDTO);

        // Assert
        String expectedDestination = "/user/12345678-1234-1234-1234-123456789012/queue/notifications";
        verify(messagingTemplate).convertAndSend(eq(expectedDestination), eq(notificationDTO));
    }

    @Test
    @DisplayName("Should send different notification types")
    void shouldSendDifferentNotificationTypes() {
        // Arrange
        NotificationDTO friendRequestNotification = NotificationDTO.builder()
                .notificationId(UUID.randomUUID())
                .recipientId(userId)
                .type(NotificationType.FRIEND_REQUEST_RECEIVED)
                .message("You have a new friend request")
                .status(NotificationStatus.UNREAD)
                .createdAt(Instant.now())
                .build();

        // Act
        webSocketService.sendNotificationToUser(userId, friendRequestNotification);

        // Assert
        String expectedDestination = "/user/" + userId + "/queue/notifications";
        verify(messagingTemplate).convertAndSend(eq(expectedDestination), eq(friendRequestNotification));
    }
}