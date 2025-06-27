package com.example.notificationmodule.service;


import com.example.notificationmodule.model.dto.NotificationDTO;
import com.example.notificationmodule.model.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public interface IDTOConversionService {
    public NotificationDTO toDTO(Notification notification);
}
