package com.example.notificationmodule.model.dto;

import com.example.notificationmodule.model.enumeration.NotificationStatus;
import com.example.notificationmodule.model.enumeration.NotificationType;
import jakarta.validation.constraints.Size;
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
    private UUID initiatorId;
    private UUID recipientId;
    private NotificationType type;
    @Size(max = 255, message = "Message must not exceed 255 characters.")
    private String message;
    private NotificationStatus status;
    private Map<String, String> attributes;
    private Instant createdAt;

    public Map<String, String> getAttributes() {
        return attributes == null ? null : Map.copyOf(attributes);
        // Or: return attributes == null ? null : new HashMap<>(attributes);
    }
}
