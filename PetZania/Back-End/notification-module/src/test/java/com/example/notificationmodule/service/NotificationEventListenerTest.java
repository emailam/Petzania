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
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
                .initiatorId(UUID.randomUUID())
                .type(NotificationType.FRIEND_REQUEST_ACCEPTED)
                .message("John Doe accepted your friend request")
                .build();

        savedNotification = Notification.builder()
                .notificationId(UUID.randomUUID())
                .recipientId(recipientId)
                .initiatorId(UUID.randomUUID())
                .type(NotificationType.FRIEND_REQUEST_ACCEPTED)
                .message("John Doe accepted your friend request")
                .status(NotificationStatus.UNREAD)
                .createdAt(Instant.now())
                .build();

        notificationDTO = NotificationDTO.builder()
                .notificationId(savedNotification.getNotificationId())
                .recipientId(recipientId)
                .initiatorId(UUID.randomUUID())
                .type(NotificationType.FRIEND_REQUEST_ACCEPTED)
                .message("John Doe accepted your friend request")
                .status(NotificationStatus.UNREAD)
                .createdAt(savedNotification.getCreatedAt())
                .build();
    }

    @Test
    @DisplayName("Should process notification event successfully")
    void shouldProcessNotificationEventSuccessfully() {
        // Arrange
        Channel mockChannel = mock(Channel.class);
        Message mockMessage = mock(Message.class);
        when(mockMessage.getMessageProperties()).thenReturn(mock(MessageProperties.class));
        when(mockMessage.getMessageProperties().getDeliveryTag()).thenReturn(123L);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(dtoConversionService.toDTO(savedNotification)).thenReturn(notificationDTO);

        // Act
        notificationEventListener.onNotificationReceived(notificationEvent, mockChannel, mockMessage);

        // Assert
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification capturedNotification = notificationCaptor.getValue();
        assertEquals(recipientId, capturedNotification.getRecipientId());
        assertEquals(NotificationType.FRIEND_REQUEST_ACCEPTED, capturedNotification.getType());
        assertEquals("John Doe accepted your friend request", capturedNotification.getMessage());

        verify(webSocketService).sendNotificationToUser(recipientId, notificationDTO);
        verify(dtoConversionService).toDTO(savedNotification);
    }

    @Test
    @DisplayName("Should handle repository exception gracefully")
    void shouldHandleRepositoryException() {
        // Arrange
        Channel mockChannel = mock(Channel.class);
        Message mockMessage = mock(Message.class);
        when(mockMessage.getMessageProperties()).thenReturn(mock(MessageProperties.class));
        when(mockMessage.getMessageProperties().getDeliveryTag()).thenReturn(123L);
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act - Should not throw exception
        notificationEventListener.onNotificationReceived(notificationEvent, mockChannel, mockMessage);

        // Assert
        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketService, never()).sendNotificationToUser(any(), any());
    }

    @Test
    @DisplayName("Should handle WebSocket exception gracefully")
    void shouldHandleWebSocketException() {
        // Arrange
        Channel mockChannel = mock(Channel.class);
        Message mockMessage = mock(Message.class);
        when(mockMessage.getMessageProperties()).thenReturn(mock(MessageProperties.class));
        when(mockMessage.getMessageProperties().getDeliveryTag()).thenReturn(123L);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(dtoConversionService.toDTO(savedNotification)).thenReturn(notificationDTO);
        doThrow(new RuntimeException("WebSocket error"))
                .when(webSocketService).sendNotificationToUser(any(), any());

        // Act - Should not throw exception
        notificationEventListener.onNotificationReceived(notificationEvent, mockChannel, mockMessage);

        // Assert
        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketService).sendNotificationToUser(recipientId, notificationDTO);
    }

    @Test
    @DisplayName("Should process event with null attributes")
    void shouldProcessEventWithNullAttributes() {
        Set<NotificationType> deletionTypes = Set.of(
                NotificationType.PET_POST_DELETED,
                NotificationType.FRIEND_REQUEST_WITHDRAWN
        );
        // Arrange
        Channel mockChannel = mock(Channel.class);
        Message mockMessage = mock(Message.class);
        when(mockMessage.getMessageProperties()).thenReturn(mock(MessageProperties.class));
        when(mockMessage.getMessageProperties().getDeliveryTag()).thenReturn(123L);
        NotificationEvent eventWithNullAttributes = NotificationEvent.builder()
                .recipientId(recipientId)
                .type(NotificationType.NEW_FOLLOWER)
                .message("You have a new follower")
                .entityId(null)
                .initiatorId(UUID.randomUUID())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(dtoConversionService.toDTO(savedNotification)).thenReturn(notificationDTO);

        // Act
        notificationEventListener.onNotificationReceived(eventWithNullAttributes, mockChannel, mockMessage);

        // Assert
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification capturedNotification = notificationCaptor.getValue();
        assertNull(capturedNotification.getEntityId());
    }

    @Test
    @DisplayName("Should process all notification types")
    void shouldProcessAllNotificationTypes() {
        Set<NotificationType> deletionTypes = Set.of(
                NotificationType.PET_POST_DELETED,
                NotificationType.FRIEND_REQUEST_WITHDRAWN
        );
        // Test each notification type
        for (NotificationType type : NotificationType.values()) {
            if (deletionTypes.contains(type)) {
                continue;
            }
            // Arrange
            Channel mockChannel = mock(Channel.class);
            Message mockMessage = mock(Message.class);
            when(mockMessage.getMessageProperties()).thenReturn(mock(MessageProperties.class));
            when(mockMessage.getMessageProperties().getDeliveryTag()).thenReturn(123L);
            NotificationEvent event = NotificationEvent.builder()
                    .recipientId(recipientId)
                    .type(type)
                    .message("Test message for " + type)
                    .initiatorId(UUID.randomUUID())
                    .build();

            when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
            when(dtoConversionService.toDTO(savedNotification)).thenReturn(notificationDTO);

            // Act
            notificationEventListener.onNotificationReceived(event, mockChannel, mockMessage);

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
        Channel mockChannel = mock(Channel.class);
        Message mockMessage = mock(Message.class);
        when(mockMessage.getMessageProperties()).thenReturn(mock(MessageProperties.class));
        when(mockMessage.getMessageProperties().getDeliveryTag()).thenReturn(123L);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(dtoConversionService.toDTO(savedNotification))
                .thenThrow(new RuntimeException("Conversion error"));

        // Act - Should not throw exception
        notificationEventListener.onNotificationReceived(notificationEvent, mockChannel, mockMessage);

        // Assert
        verify(notificationRepository).save(any(Notification.class));
        verify(dtoConversionService).toDTO(savedNotification);
        verify(webSocketService, never()).sendNotificationToUser(any(), any());
    }
}