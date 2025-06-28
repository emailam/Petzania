package com.example.friends.and.chats.module.model.event;

import com.example.friends.and.chats.module.model.enumeration.NotificationType;
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
    private UUID recipientId;
    private NotificationType type;
    private String message;
    private Map<String, String> attributes;

}
