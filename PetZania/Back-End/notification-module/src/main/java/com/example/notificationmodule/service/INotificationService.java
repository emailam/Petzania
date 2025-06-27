package com.example.notificationmodule.service;

import com.example.notificationmodule.model.dto.NotificationDTO;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface INotificationService {
    Page<NotificationDTO> getNotificationsByRecipient(UUID recipientId, int page, int size, String sortBy, String direction);

    int getUnreadNotificationCount(UUID recipientId);

    boolean markAsRead(UUID ownerId, UUID notificationId);

    boolean markAllRead(UUID ownerId);

    boolean deleteNotification(UUID ownerId, UUID notificationId);
}
