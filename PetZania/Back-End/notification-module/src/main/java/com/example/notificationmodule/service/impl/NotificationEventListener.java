package com.example.notificationmodule.service.impl;

import com.example.notificationmodule.model.entity.Notification;
import com.example.notificationmodule.model.enumeration.NotificationType;
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
    public void onNotificationReceived(NotificationEvent event) {
        log.info("Received notification event: {} for recipient: {}", event.getType(), event.getRecipientId());

        try {
            if (event.getType() == NotificationType.PET_POST_DELETED) {
                notificationRepository.deleteByEntityId(event.getEntityId());
                log.info("Deleted notifications for deleted post {}", event.getEntityId());
                return;
            }
            if (event.getType() == NotificationType.FRIEND_REQUEST_WITHDRAWN) {
                notificationRepository.deleteByEntityId(event.getEntityId());
                log.info("Deleted notifications for withdrawn friend request {}", event.getEntityId());
                return;
            }
            if (event.getInitiatorId() == event.getRecipientId()) {
                // Ignore notifications to yourself.
                return;
            }
            Notification notification = Notification.builder()
                    .recipientId(event.getRecipientId())
                    .initiatorId(event.getInitiatorId())
                    .entityId(event.getEntityId())
                    .type(event.getType())
                    .message(event.getMessage())
                    .build();

            Notification savedNotification = notificationRepository.save(notification);

            // send real-time notification via the defined websocket
            webSocketService.sendNotificationToUser(event.getRecipientId(), dtoConversionService.toDTO(savedNotification));

            System.out.println("received new notification: " + savedNotification);
            log.info("Notification processed successfully with ID: {}", savedNotification.getNotificationId());
        } catch (Exception e) {
            log.error("Error processing notification event: {}", event, e);
            // might want to send to a dead letter queue here
        }
    }
}
