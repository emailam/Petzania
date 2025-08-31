package com.example.notificationmodule.service.impl;

import com.example.notificationmodule.model.entity.User;
import com.example.notificationmodule.model.event.UserEvent;
import com.example.notificationmodule.repository.NotificationRepository;
import com.example.notificationmodule.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import lombok.extern.slf4j.Slf4j;

import static com.example.notificationmodule.constant.Constants.*;

@Service
@AllArgsConstructor
@Transactional
@Slf4j
public class UserEventListener {
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @RabbitListener(queues = USER_REGISTERED_QUEUE_NOTIFICATION_MODULE, ackMode = ACK_MODE)
    public void onUserRegistered(UserEvent user, Channel channel, Message message) {
        try {
            if (!userRepository.existsById(user.getUserId()) && !userRepository.existsByUsername(user.getUsername()) && !userRepository.existsByEmail(user.getEmail())) {
                User newUser = User.builder()
                        .userId(user.getUserId())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .build();
                userRepository.save(newUser);
                log.info("received registered user: {}", user);
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception ex) {
            log.error("Error processing user registered event: {}", user, ex);
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            } catch (Exception nackErr) {
                log.error("Error nacking message for event: {}", user, nackErr);
            }
        }
    }

    @RabbitListener(queues = USER_DELETED_QUEUE_NOTIFICATION_MODULE, ackMode = ACK_MODE)
    public void onUserDeleted(UserEvent user, Channel channel, Message message) {
        try {
            if (userRepository.existsById(user.getUserId())) {
                notificationRepository.deleteByRecipientId(user.getUserId());
                notificationRepository.deleteByInitiatorId(user.getUserId());
                userRepository.deleteById(user.getUserId());
                log.info("received deleted user: {}", user);
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception ex) {
            log.error("Error processing user deleted event: {}", user, ex);
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            } catch (Exception nackErr) {
                log.error("Error nacking message for event: {}", user, nackErr);
            }
        }
    }
}
