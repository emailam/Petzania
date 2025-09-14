package com.example.notificationmodule.service;

import com.example.notificationmodule.model.dto.NotificationDTO;
import com.example.notificationmodule.model.entity.Notification;
import com.example.notificationmodule.model.enumeration.NotificationStatus;
import com.example.notificationmodule.model.enumeration.NotificationType;
import com.example.notificationmodule.model.event.NotificationEvent;
import com.example.notificationmodule.repository.NotificationRepository;
import com.example.notificationmodule.repository.UserRepository;
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

    @Mock
    private UserRepository userRepository;
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
        when(userRepository.existsById(notificationEvent.getRecipientId())).thenReturn(true);
        when(userRepository.existsById(notificationEvent.getInitiatorId())).thenReturn(true);
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
        when(userRepository.existsById(notificationEvent.getRecipientId())).thenReturn(true);
        when(userRepository.existsById(notificationEvent.getInitiatorId())).thenReturn(true);
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
        when(userRepository.existsById(notificationEvent.getRecipientId())).thenReturn(true);
        when(userRepository.existsById(notificationEvent.getInitiatorId())).thenReturn(true);
        doThrow(new RuntimeException("WebSocket error"))
                .when(webSocketService).sendNotificationToUser(any(), any());

        // Act - Should not throw exception
        notificationEventListener.onNotificationReceived(notificationEvent, mockChannel, mockMessage);

        // Assert
        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketService).sendNotificationToUser(recipientId, notificationDTO);
    }
    @Test
    @DisplayName("Should handle conversion service exception")
    void shouldHandleConversionServiceException() {
        // Arrange
        Channel mockChannel = mock(Channel.class);
        Message mockMessage = mock(Message.class);
        when(userRepository.existsById(notificationEvent.getRecipientId())).thenReturn(true);
        when(userRepository.existsById(notificationEvent.getInitiatorId())).thenReturn(true);
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