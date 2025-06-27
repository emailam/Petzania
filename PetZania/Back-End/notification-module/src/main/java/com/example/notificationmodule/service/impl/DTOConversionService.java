package com.example.notificationmodule.service.impl;

import com.example.notificationmodule.model.dto.NotificationDTO;
import com.example.notificationmodule.model.entity.Notification;
import com.example.notificationmodule.service.IDTOConversionService;

public class DTOConversionService implements IDTOConversionService {
    @Override
    public NotificationDTO toDTO(Notification notification) {
        return NotificationDTO.builder()
                .notificationId(notification.getNotificationId())
                .recipientId(notification.getRecipientId())
                .type(notification.getType())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .attributes(notification.getAttributes())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
