package com.example.notificationmodule.service;


import com.example.notificationmodule.exception.notification.NotificationNotFound;
import com.example.notificationmodule.exception.user.UserNotFound;
import com.example.notificationmodule.model.dto.NotificationDTO;
import com.example.notificationmodule.model.entity.Notification;
import com.example.notificationmodule.model.enumeration.NotificationStatus;
import com.example.notificationmodule.model.enumeration.NotificationType;
import com.example.notificationmodule.repository.NotificationRepository;
import com.example.notificationmodule.repository.UserRepository;
import com.example.notificationmodule.service.impl.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IDTOConversionService dtoConversionService;

    @Mock
    private IWebSocketService webSocketService;

    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;
    private UUID notificationId;
    private Notification notification;
    private NotificationDTO notificationDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();

        notification = Notification.builder()
                .notificationId(notificationId)
                .recipientId(userId)
                .initiatorId(userId)
                .type(NotificationType.FRIEND_REQUEST_ACCEPTED)
                .message("Test notification")
                .status(NotificationStatus.UNREAD)
                .createdAt(Instant.now())
                .build();

        notificationDTO = NotificationDTO.builder()
                .notificationId(notificationId)
                .recipientId(userId)
                .type(NotificationType.FRIEND_REQUEST_ACCEPTED)
                .message("Test notification")
                .status(NotificationStatus.UNREAD)
                .createdAt(notification.getCreatedAt())
                .build();
    }

    @Nested
    @DisplayName("getNotificationsByRecipient Tests")
    class GetNotificationsByRecipientTests {

        @Test
        @DisplayName("Should return paginated notifications successfully")
        void shouldReturnPaginatedNotifications() {
            // Arrange
            when(userRepository.existsById(userId)).thenReturn(true);

            List<Notification> notifications = Arrays.asList(notification);
            Page<Notification> notificationPage = new PageImpl<>(notifications);
            Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

            when(notificationRepository.findByRecipientId(eq(userId), any(Pageable.class)))
                    .thenReturn(notificationPage);
            when(dtoConversionService.toDTO(notification)).thenReturn(notificationDTO);

            // Act
            Page<NotificationDTO> result = notificationService.getNotificationsByRecipient(
                    userId, 0, 10, "createdAt", "desc");

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(notificationDTO, result.getContent().get(0));

            verify(userRepository).existsById(userId);
            verify(notificationRepository).findByRecipientId(eq(userId), any(Pageable.class));
            verify(dtoConversionService).toDTO(notification);
        }

        @Test
        @DisplayName("Should handle ascending sort direction")
        void shouldHandleAscendingSort() {
            // Arrange
            when(userRepository.existsById(userId)).thenReturn(true);
            when(notificationRepository.findByRecipientId(eq(userId), any(Pageable.class)))
                    .thenReturn(Page.empty());

            // Act
            notificationService.getNotificationsByRecipient(userId, 0, 10, "createdAt", "asc");

            // Assert
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(notificationRepository).findByRecipientId(eq(userId), pageableCaptor.capture());

            Sort.Order order = pageableCaptor.getValue().getSort().iterator().next();
            assertEquals(Sort.Direction.ASC, order.getDirection());
        }

        @Test
        @DisplayName("Should throw UserNotFound when user doesn't exist")
        void shouldThrowUserNotFound() {
            // Arrange
            when(userRepository.existsById(userId)).thenReturn(false);

            // Act & Assert
            UserNotFound exception = assertThrows(UserNotFound.class, () ->
                    notificationService.getNotificationsByRecipient(userId, 0, 10, "createdAt", "desc"));

            assertEquals("User does not Exist", exception.getMessage());
            verify(notificationRepository, never()).findByRecipientId(any(), any());
        }

        @Test
        @DisplayName("Should handle empty notification list")
        void shouldHandleEmptyNotifications() {
            // Arrange
            when(userRepository.existsById(userId)).thenReturn(true);
            when(notificationRepository.findByRecipientId(eq(userId), any(Pageable.class)))
                    .thenReturn(Page.empty());

            // Act
            Page<NotificationDTO> result = notificationService.getNotificationsByRecipient(
                    userId, 0, 10, "createdAt", "desc");

            // Assert
            assertTrue(result.isEmpty());
            assertEquals(0, result.getTotalElements());
        }

        @Test
        @DisplayName("Should handle case-insensitive sort direction")
        void shouldHandleCaseInsensitiveSortDirection() {
            // Arrange
            when(userRepository.existsById(userId)).thenReturn(true);
            when(notificationRepository.findByRecipientId(eq(userId), any(Pageable.class)))
                    .thenReturn(Page.empty());

            // Act
            notificationService.getNotificationsByRecipient(userId, 0, 10, "createdAt", "DESC");

            // Assert
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(notificationRepository).findByRecipientId(eq(userId), pageableCaptor.capture());

            Sort.Order order = pageableCaptor.getValue().getSort().iterator().next();
            assertEquals(Sort.Direction.DESC, order.getDirection());
        }
    }

    @Nested
    @DisplayName("getUnreadNotificationCount Tests")
    class GetUnreadNotificationCountTests {

        @Test
        @DisplayName("Should return correct unread count")
        void shouldReturnUnreadCount() {
            // Arrange
            when(userRepository.existsById(userId)).thenReturn(true);
            when(notificationRepository.countByRecipientIdAndStatus(userId, NotificationStatus.UNREAD))
                    .thenReturn(5);

            // Act
            int count = notificationService.getUnreadNotificationCount(userId);

            // Assert
            assertEquals(5, count);
            verify(userRepository).existsById(userId);
            verify(notificationRepository).countByRecipientIdAndStatus(userId, NotificationStatus.UNREAD);
        }

        @Test
        @DisplayName("Should return zero when no unread notifications")
        void shouldReturnZeroWhenNoUnread() {
            // Arrange
            when(userRepository.existsById(userId)).thenReturn(true);
            when(notificationRepository.countByRecipientIdAndStatus(userId, NotificationStatus.UNREAD))
                    .thenReturn(0);

            // Act
            int count = notificationService.getUnreadNotificationCount(userId);

            // Assert
            assertEquals(0, count);
        }

        @Test
        @DisplayName("Should throw UserNotFound when user doesn't exist")
        void shouldThrowUserNotFoundForCount() {
            // Arrange
            when(userRepository.existsById(userId)).thenReturn(false);

            // Act & Assert
            assertThrows(UserNotFound.class, () ->
                    notificationService.getUnreadNotificationCount(userId));

            verify(notificationRepository, never()).countByRecipientIdAndStatus(any(), any());
        }
    }

    @Nested
    @DisplayName("markAsRead Tests")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should mark notification as read successfully")
        void shouldMarkAsReadSuccessfully() {
            // Arrange
            when(userRepository.existsById(userId)).thenReturn(true);
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
            when(notificationRepository.markAsRead(notificationId)).thenReturn(1);
            when(notificationRepository.countByRecipientIdAndStatus(userId, NotificationStatus.UNREAD))
                    .thenReturn(3);

            // Act
            boolean result = notificationService.markAsRead(userId, notificationId);

            // Assert
            assertTrue(result);
            verify(notificationRepository).markAsRead(notificationId);
            verify(webSocketService).sendNotificationCountUpdate(userId, 3);
        }

        @Test
        @DisplayName("Should return false when notification already read")
        void shouldReturnFalseWhenAlreadyRead() {
            // Arrange
            when(userRepository.existsById(userId)).thenReturn(true);
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
            when(notificationRepository.markAsRead(notificationId)).thenReturn(0);

            // Act
            boolean result = notificationService.markAsRead(userId, notificationId);

            // Assert
            assertFalse(result);
            verify(webSocketService, never()).sendNotificationCountUpdate(any(), anyLong());
        }

        @Test
        @DisplayName("Should return false when notification belongs to different user")
        void shouldReturnFalseWhenDifferentUser() {
            // Arrange
            UUID differentUserId = UUID.randomUUID();
            notification.setRecipientId(differentUserId);

            when(userRepository.existsById(userId)).thenReturn(true);
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

            // Act
            boolean result = notificationService.markAsRead(userId, notificationId);

            // Assert
            assertFalse(result);
            verify(notificationRepository, never()).markAsRead(any());
            verify(webSocketService, never()).sendNotificationCountUpdate(any(), anyLong());
        }

        @Test
        @DisplayName("Should throw NotificationNotFound when notification doesn't exist")
        void shouldThrowNotificationNotFound() {
            // Arrange
            when(userRepository.existsById(userId)).thenReturn(true);
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

            // Act & Assert
            NotificationNotFound exception = assertThrows(NotificationNotFound.class, () ->
                    notificationService.markAsRead(userId, notificationId));

            assertEquals("Notification does not exist", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw UserNotFound when user doesn't exist")
        void shouldThrowUserNotFoundForMarkAsRead() {
            // Arrange
            when(userRepository.existsById(userId)).thenReturn(false);

            // Act & Assert
            assertThrows(UserNotFound.class, () ->
                    notificationService.markAsRead(userId, notificationId));
        }
    }

    @Nested
    @DisplayName("markAllRead Tests")
    class MarkAllReadTests {

        @Test
        @DisplayName("Should mark all notifications as read successfully")
        void shouldMarkAllAsReadSuccessfully() {
            // Arrange
            when(userRepository.existsById(userId)).thenReturn(true);
            when(notificationRepository.markAllAsRead(userId)).thenReturn(5);
            when(notificationRepository.countByRecipientIdAndStatus(userId, NotificationStatus.UNREAD))
                    .thenReturn(0);

            // Act
            boolean result = notificationService.markAllRead(userId);

            // Assert
            assertTrue(result);
            verify(notificationRepository).markAllAsRead(userId);
            verify(webSocketService).sendNotificationCountUpdate(userId, 0);
        }

        @Test
        @DisplayName("Should return false when no unread notifications")
        void shouldReturnFalseWhenNoUnread() {
            // Arrange
            when(userRepository.existsById(userId)).thenReturn(true);
            when(notificationRepository.markAllAsRead(userId)).thenReturn(0);

            // Act
            boolean result = notificationService.markAllRead(userId);

            // Assert
            assertFalse(result);
            verify(webSocketService, never()).sendNotificationCountUpdate(any(), anyLong());
        }

        @Test
        @DisplayName("Should throw UserNotFound when user doesn't exist")
        void shouldThrowUserNotFoundForMarkAllRead() {
            // Arrange
            when(userRepository.existsById(userId)).thenReturn(false);

            // Act & Assert
            assertThrows(UserNotFound.class, () ->
                    notificationService.markAllRead(userId));
        }
    }

    @Nested
    @DisplayName("deleteNotification Tests")
    class DeleteNotificationTests {

        @Test
        @DisplayName("Should delete notification successfully")
        void shouldDeleteSuccessfully() {
            // Arrange
            when(userRepository.existsById(userId)).thenReturn(true);
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
            when(notificationRepository.countByRecipientIdAndStatus(userId, NotificationStatus.UNREAD))
                    .thenReturn(2);

            // Act
            boolean result = notificationService.deleteNotification(userId, notificationId);

            // Assert
            assertTrue(result);
            verify(notificationRepository).deleteByNotificationId(notificationId);
            verify(webSocketService).sendNotificationCountUpdate(userId, 2);
        }

        @Test
        @DisplayName("Should return false when notification belongs to different user")
        void shouldReturnFalseWhenDifferentUserDelete() {
            // Arrange
            UUID differentUserId = UUID.randomUUID();
            notification.setRecipientId(differentUserId);

            when(userRepository.existsById(userId)).thenReturn(true);
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

            // Act
            boolean result = notificationService.deleteNotification(userId, notificationId);

            // Assert
            assertFalse(result);
            verify(notificationRepository, never()).deleteByNotificationId(any());
            verify(webSocketService, never()).sendNotificationCountUpdate(any(), anyLong());
        }

        @Test
        @DisplayName("Should throw NotificationNotFound when notification doesn't exist")
        void shouldThrowNotificationNotFoundForDelete() {
            // Arrange
            when(userRepository.existsById(userId)).thenReturn(true);
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

            // Act & Assert
            NotificationNotFound exception = assertThrows(NotificationNotFound.class, () ->
                    notificationService.deleteNotification(userId, notificationId));

            assertEquals("Notification does not exist", exception.getMessage());
        }

        @Test
        @DisplayName("Should delete and update count even if notification was read")
        void shouldDeleteReadNotification() {
            // Arrange
            notification.setStatus(NotificationStatus.READ);
            when(userRepository.existsById(userId)).thenReturn(true);
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
            when(notificationRepository.countByRecipientIdAndStatus(userId, NotificationStatus.UNREAD))
                    .thenReturn(0);

            // Act
            boolean result = notificationService.deleteNotification(userId, notificationId);

            // Assert
            assertTrue(result);
            verify(notificationRepository).deleteByNotificationId(notificationId);
            verify(webSocketService).sendNotificationCountUpdate(userId, 0);
        }
    }
}