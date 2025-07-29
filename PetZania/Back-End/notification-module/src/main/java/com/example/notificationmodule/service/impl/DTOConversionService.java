package com.example.notificationmodule.service.impl;

import com.example.notificationmodule.model.dto.NotificationDTO;
import com.example.notificationmodule.model.entity.Notification;
import com.example.notificationmodule.service.IDTOConversionService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DTOConversionService implements IDTOConversionService {
    @Override
    public NotificationDTO toDTO(Notification notification) {
        return NotificationDTO.builder()
                .notificationId(notification.getNotificationId())
                .recipientId(notification.getRecipientId())
                .entityId(notification.getEntityId())
                .type(notification.getType())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .createdAt(notification.getCreatedAt())
                .initiatorId(notification.getInitiatorId())
                .build();
    }
}
