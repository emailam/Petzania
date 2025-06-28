package com.example.notificationmodule;

import com.example.notificationmodule.model.entity.Notification;
import com.example.notificationmodule.model.entity.User;
import com.example.notificationmodule.model.enumeration.NotificationStatus;
import com.example.notificationmodule.model.enumeration.NotificationType;

import java.time.Instant;
import java.util.UUID;

public class TestDataUtil {
    public TestDataUtil() {

    }
    public static User createTestUser(String username){
        return User.builder()
                .userId(UUID.randomUUID())
                .username(username)
                .email(username + "@gmail.com")
                .build();
    }
    public static Notification createTestNotification(UUID recipientId, NotificationType notificationType, String message, NotificationStatus notificationStatus, Instant instant){
        return Notification.builder()
                .recipientId(recipientId)
                .type(notificationType)
                .message(message)
                .status(notificationStatus)
                .createdAt(instant)
                .build();
    }
}