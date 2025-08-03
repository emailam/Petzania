package com.example.adoption_and_breeding_module.model.event;

import com.example.adoption_and_breeding_module.model.enumeration.NotificationType;
import jakarta.validation.constraints.Size;
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
    private UUID entityId;
    private NotificationType type;

    @Size(max = 255, message = "Message size shouldn't exceed 255 characters.")
    private String message;
}
