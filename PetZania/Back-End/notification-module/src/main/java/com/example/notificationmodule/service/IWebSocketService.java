package com.example.notificationmodule.service;

import com.example.notificationmodule.model.dto.NotificationDTO;

import java.util.UUID;

public interface IWebSocketService {
    void sendNotificationToUser(UUID userId, NotificationDTO notificationDTO);
    void sendNotificationCountUpdate(UUID userId, long unreadCount);
}
