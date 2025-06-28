package com.example.notificationmodule.service;

import com.example.notificationmodule.model.dto.NotificationDTO;
import com.example.notificationmodule.model.entity.Notification;
import com.example.notificationmodule.model.enumeration.NotificationStatus;
import com.example.notificationmodule.model.enumeration.NotificationType;
import com.example.notificationmodule.model.event.NotificationEvent;
import com.example.notificationmodule.repository.NotificationRepository;
import com.example.notificationmodule.service.impl.NotificationEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private IWebSocketService webSocketService;

    @Mock
    private IDTOConversionService dtoConversionService;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    private NotificationEvent notificationEvent;
    private Notification savedNotification;
    private NotificationDTO notificationDTO;
    private UUID recipientId;

    @BeforeEach
    void setUp() {
        recipientId = UUID.randomUUID();
        Map<String, String> attributes = new HashMap<>();
        attributes.put("friendName", "John Doe");

        notificationEvent = NotificationEvent.builder()
                .recipientId(recipientId)
                .type(NotificationType.FRIEND_REQUEST_ACCEPTED)
                .message("John Doe accepted your friend request")
                .attributes(attributes)
                .build();

        savedNotification = Notification.builder()
                .notificationId(UUID.randomUUID())
                .recipientId(recipientId)
                .type(NotificationType.FRIEND_REQUEST_ACCEPTED)
                .message("John Doe accepted your friend request")
                .status(NotificationStatus.UNREAD)
                .attributes(attributes)
                .createdAt(Instant.now())
                .build();

        notificationDTO = NotificationDTO.builder()
                .notificationId(savedNotification.getNotificationId())
                .recipientId(recipientId)
                .type(NotificationType.FRIEND_REQUEST_ACCEPTED)
                .message("John Doe accepted your friend request")
                .status(NotificationStatus.UNREAD)
                .attributes(attributes)
                .createdAt(savedNotification.getCreatedAt())
                .build();
    }

    @Test
    @DisplayName("Should process notification event successfully")
    void shouldProcessNotificationEventSuccessfully() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(dtoConversionService.toDTO(savedNotification)).thenReturn(notificationDTO);

        // Act
        notificationEventListener.onNotificationReceived(notificationEvent);

        // Assert
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification capturedNotification = notificationCaptor.getValue();
        assertEquals(recipientId, capturedNotification.getRecipientId());
        assertEquals(NotificationType.FRIEND_REQUEST_ACCEPTED, capturedNotification.getType());
        assertEquals("John Doe accepted your friend request", capturedNotification.getMessage());
        assertEquals(notificationEvent.getAttributes(), capturedNotification.getAttributes());

        verify(webSocketService).sendNotificationToUser(recipientId, notificationDTO);
        verify(dtoConversionService).toDTO(savedNotification);
    }

    @Test
    @DisplayName("Should handle repository exception gracefully")
    void shouldHandleRepositoryException() {
        // Arrange
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act - Should not throw exception
        notificationEventListener.onNotificationReceived(notificationEvent);

        // Assert
        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketService, never()).sendNotificationToUser(any(), any());
    }

    @Test
    @DisplayName("Should handle WebSocket exception gracefully")
    void shouldHandleWebSocketException() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(dtoConversionService.toDTO(savedNotification)).thenReturn(notificationDTO);
        doThrow(new RuntimeException("WebSocket error"))
                .when(webSocketService).sendNotificationToUser(any(), any());

        // Act - Should not throw exception
        notificationEventListener.onNotificationReceived(notificationEvent);

        // Assert
        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketService).sendNotificationToUser(recipientId, notificationDTO);
    }

    @Test
    @DisplayName("Should process event with null attributes")
    void shouldProcessEventWithNullAttributes() {
        // Arrange
        NotificationEvent eventWithNullAttributes = NotificationEvent.builder()
                .recipientId(recipientId)
                .type(NotificationType.NEW_FOLLOWER)
                .message("You have a new follower")
                .attributes(null)
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(dtoConversionService.toDTO(savedNotification)).thenReturn(notificationDTO);

        // Act
        notificationEventListener.onNotificationReceived(eventWithNullAttributes);

        // Assert
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification capturedNotification = notificationCaptor.getValue();
        assertNull(capturedNotification.getAttributes());
    }

    @Test
    @DisplayName("Should process all notification types")
    void shouldProcessAllNotificationTypes() {
        // Test each notification type
        for (NotificationType type : NotificationType.values()) {
            // Arrange
            NotificationEvent event = NotificationEvent.builder()
                    .recipientId(recipientId)
                    .type(type)
                    .message("Test message for " + type)
                    .build();

            when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
            when(dtoConversionService.toDTO(savedNotification)).thenReturn(notificationDTO);

            // Act
            notificationEventListener.onNotificationReceived(event);

            // Assert
            ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository, atLeastOnce()).save(notificationCaptor.capture());

            boolean found = notificationCaptor.getAllValues().stream()
                    .anyMatch(n -> n.getType() == type);
            assertTrue(found, "Notification type " + type + " was not processed");
        }
    }

    @Test
    @DisplayName("Should handle conversion service exception")
    void shouldHandleConversionServiceException() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(dtoConversionService.toDTO(savedNotification))
                .thenThrow(new RuntimeException("Conversion error"));

        // Act - Should not throw exception
        notificationEventListener.onNotificationReceived(notificationEvent);

        // Assert
        verify(notificationRepository).save(any(Notification.class));
        verify(dtoConversionService).toDTO(savedNotification);
        verify(webSocketService, never()).sendNotificationToUser(any(), any());
    }
}