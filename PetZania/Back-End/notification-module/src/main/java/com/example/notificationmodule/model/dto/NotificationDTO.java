package com.example.notificationmodule.model.dto;
import com.example.notificationmodule.model.enumeration.NotificationStatus;
import com.example.notificationmodule.model.enumeration.NotificationType;
import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    private UUID notificationId;
    private UUID recipientId;
    private NotificationType type;
    private String message;
    private NotificationStatus status;
    private Map<String, String> attributes;
    private Instant createdAt;
}
