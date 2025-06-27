package com.example.notificationmodule.service.impl;

import com.example.notificationmodule.model.entity.Notification;
import com.example.notificationmodule.model.event.NotificationEvent;
import com.example.notificationmodule.repository.NotificationRepository;
import com.example.notificationmodule.service.IDTOConversionService;
import com.example.notificationmodule.service.IWebSocketService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {
    private final NotificationRepository notificationRepository;
    private final IWebSocketService webSocketService;
    private final IDTOConversionService dtoConversionService;

    @RabbitListener(queues = "notificationsQueue")
    @Transactional
    public void onNotificationReceived(NotificationEvent event){
        log.info("Received notification event: {} for recipient: {}", event.getType(), event.getRecipientId());

        try {
            Notification notification = Notification.builder()
                    .recipientId(event.getRecipientId())
                    .type(event.getType())
                    .message(event.getMessage())
                    .attributes(event.getAttributes())
                    .build();

            Notification savedNotification = notificationRepository.save(notification);

            // send real-time notification via the defined websocket
            webSocketService.sendNotificationToUser(
                    event.getRecipientId(),
                    dtoConversionService.toDTO(savedNotification)
            );

            log.info("Notification processed successfully with ID: {}", savedNotification.getNotificationId());

        } catch(Exception e){
            log.error("Error processing notification event: {}", event, e);
            // might want to send to a dead letter queue here
        }
    }
}
