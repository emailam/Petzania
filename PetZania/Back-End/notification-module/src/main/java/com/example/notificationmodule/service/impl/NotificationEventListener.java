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
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

import static com.example.notificationmodule.constant.Constants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {
    private final NotificationRepository notificationRepository;
    private final IWebSocketService webSocketService;
    private final IDTOConversionService dtoConversionService;

    @Transactional
    @RabbitListener(queues = NOTIFICATIONS_QUEUE, ackMode = ACK_MODE)
    public void onNotificationReceived(NotificationEvent event, Channel channel, Message message) {
        try {
            if (event.getType() == NotificationType.PET_POST_DELETED || event.getType() == NotificationType.FRIEND_REQUEST_WITHDRAWN) {
                notificationRepository.deleteByEntityId(event.getEntityId());
                log.info("Deleted notifications for event {}", event);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }
            if (event.getInitiatorId() == event.getRecipientId()) {
                // Ignore notifications to yourself.
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
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

            log.info("Received new notification: {}", savedNotification);
            log.info("Notification processed successfully with ID: {}", savedNotification.getNotificationId());
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception ex) {
            log.error("Error processing notification event: {}", event, ex);
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            } catch (Exception nackErr) {
                log.error("Error nacking message for event: {}", event, nackErr);
            }
        }
    }
}
