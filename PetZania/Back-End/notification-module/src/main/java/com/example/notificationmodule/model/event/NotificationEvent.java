package com.example.notificationmodule.model.event;

import com.example.notificationmodule.model.enumeration.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {
    private UUID initiatorId;
    private UUID recipientId;
    private NotificationType type;
    private String message;
    private Map<String, String> attributes;

    public Map<String, String> getAttributes() {
        return attributes == null ? null : Map.copyOf(attributes);
    }
}
