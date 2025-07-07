package com.example.notificationmodule.service;

import com.example.notificationmodule.model.dto.NotificationDTO;
import com.example.notificationmodule.model.entity.Notification;
import com.example.notificationmodule.model.enumeration.NotificationStatus;
import com.example.notificationmodule.model.enumeration.NotificationType;
import com.example.notificationmodule.service.impl.DTOConversionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DTOConversionServiceTest {

    private DTOConversionService dtoConversionService;

    @BeforeEach
    void setUp() {
        dtoConversionService = new DTOConversionService();
    }

    @Test
    @DisplayName("Should convert notification to DTO with all fields")
    void shouldConvertNotificationToDTOWithAllFields() {
        // Arrange
        UUID notificationId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        Map<String, String> attributes = new HashMap<>();
        attributes.put("friendName", "John Doe");
        attributes.put("postId", "12345");

        Notification notification = Notification.builder()
                .notificationId(notificationId)
                .recipientId(recipientId)
                .initiatorId(recipientId)
                .type(NotificationType.FRIEND_REQUEST_ACCEPTED)
                .message("John Doe accepted your friend request")
                .status(NotificationStatus.UNREAD)
                .attributes(attributes)
                .createdAt(createdAt)
                .build();

        // Act
        NotificationDTO dto = dtoConversionService.toDTO(notification);

        // Assert
        assertNotNull(dto);
        assertEquals(notificationId, dto.getNotificationId());
        assertEquals(recipientId, dto.getRecipientId());
        assertEquals(NotificationType.FRIEND_REQUEST_ACCEPTED, dto.getType());
        assertEquals("John Doe accepted your friend request", dto.getMessage());
        assertEquals(NotificationStatus.UNREAD, dto.getStatus());
        assertEquals(attributes, dto.getAttributes());
        assertEquals(createdAt, dto.getCreatedAt());
    }

    @Test
    @DisplayName("Should handle notification with null attributes")
    void shouldHandleNotificationWithNullAttributes() {
        // Arrange
        Notification notification = Notification.builder()
                .notificationId(UUID.randomUUID())
                .recipientId(UUID.randomUUID())
                .type(NotificationType.NEW_FOLLOWER)
                .message("You have a new follower")
                .status(NotificationStatus.READ)
                .attributes(null)
                .createdAt(Instant.now())
                .build();

        // Act
        NotificationDTO dto = dtoConversionService.toDTO(notification);

        // Assert
        assertNotNull(dto);
        assertNull(dto.getAttributes());
    }

    @Test
    @DisplayName("Should handle notification with empty attributes")
    void shouldHandleNotificationWithEmptyAttributes() {
        // Arrange
        Notification notification = Notification.builder()
                .notificationId(UUID.randomUUID())
                .recipientId(UUID.randomUUID())
                .type(NotificationType.PET_POST_LIKED)
                .message("Your pet post was liked")
                .status(NotificationStatus.UNREAD)
                .attributes(new HashMap<>())
                .createdAt(Instant.now())
                .build();

        // Act
        NotificationDTO dto = dtoConversionService.toDTO(notification);

        // Assert
        assertNotNull(dto);
        assertNotNull(dto.getAttributes());
        assertTrue(dto.getAttributes().isEmpty());
    }

    @Test
    @DisplayName("Should handle null notification")
    void shouldHandleNullNotification() {
        // Act & Assert
        assertThrows(NullPointerException.class, () ->
                dtoConversionService.toDTO(null));
    }

    @Test
    @DisplayName("Should preserve all notification types")
    void shouldPreserveAllNotificationTypes() {
        // Test each notification type
        for (NotificationType type : NotificationType.values()) {
            // Arrange
            Notification notification = Notification.builder()
                    .notificationId(UUID.randomUUID())
                    .recipientId(UUID.randomUUID())
                    .type(type)
                    .message("Test message for " + type)
                    .status(NotificationStatus.UNREAD)
                    .createdAt(Instant.now())
                    .build();

            // Act
            NotificationDTO dto = dtoConversionService.toDTO(notification);

            // Assert
            assertEquals(type, dto.getType());
        }
    }

    @Test
    @DisplayName("Should preserve all notification statuses")
    void shouldPreserveAllNotificationStatuses() {
        // Test each notification status
        for (NotificationStatus status : NotificationStatus.values()) {
            // Arrange
            Notification notification = Notification.builder()
                    .notificationId(UUID.randomUUID())
                    .recipientId(UUID.randomUUID())
                    .type(NotificationType.NEW_FOLLOWER)
                    .message("Test message")
                    .status(status)
                    .createdAt(Instant.now())
                    .build();

            // Act
            NotificationDTO dto = dtoConversionService.toDTO(notification);

            // Assert
            assertEquals(status, dto.getStatus());
        }
    }
}
