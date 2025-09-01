package com.example.adoption_and_breeding_module.service.impl;

import com.example.adoption_and_breeding_module.model.event.UserEvent;
import com.example.adoption_and_breeding_module.model.entity.User;
import com.example.adoption_and_breeding_module.repository.UserRepository;
import com.example.adoption_and_breeding_module.util.QueueUtils;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import lombok.extern.slf4j.Slf4j;

import static com.example.adoption_and_breeding_module.constant.Constants.*;

@Service
@AllArgsConstructor
@Transactional
@Slf4j
public class UserListener {
    private final UserRepository userRepository;
    private final QueueUtils queueUtils;

    @RabbitListener(queues = USER_REGISTERED_QUEUE_ADOPTION_MODULE, ackMode = ACK_MODE)
    public void onUserRegistered(UserEvent user, Channel channel, Message message) {
        try {
            if (!userRepository.existsById(user.getUserId()) && !userRepository.existsByUsername(user.getUsername())
                    && !userRepository.existsByEmail(user.getEmail())) {
                User newUser = new User();
                newUser.setUserId(user.getUserId());
                newUser.setUsername(user.getUsername());
                newUser.setEmail(user.getEmail());
                userRepository.save(newUser);
                log.info("Received registered user: {}", user);
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception ex) {
            log.error("Error processing user registered event: {}", user, ex);
            try {
                int retryCount = queueUtils.getRetryCount(message, USER_REGISTERED_QUEUE_ADOPTION_MODULE_RETRY);
                if (retryCount >= MAX_RETRIES) {
                    // simply drop the message
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    log.info("Max retries reached for the event: {}", user);
                } else {
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
                    log.info("This is the retry number: {}", retryCount);
                }
            } catch (Exception nackErr) {
                log.error("Error nacking message for event: {}", user, nackErr);
            }
        }
    }

    @RabbitListener(queues = USER_DELETED_QUEUE_ADOPTION_MODULE, ackMode = ACK_MODE)
    public void onUserDeleted(UserEvent user, Channel channel, Message message) {
        try {
            if (userRepository.existsById(user.getUserId())) {
                userRepository.deleteById(user.getUserId());
                log.info("Received deleted user: {}", user);
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception ex) {
            log.error("Error processing user deleted event: {}", user, ex);
            try {
                int retryCount = queueUtils.getRetryCount(message, USER_DELETED_QUEUE_ADOPTION_MODULE_RETRY);
                if (retryCount >= MAX_RETRIES) {
                    // simply drop the message
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    log.info("Max retries reached for the event: {}", user);
                } else {
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
                    log.info("This is the retry number: {}", retryCount);
                }
            } catch (Exception nackErr) {
                log.error("Error nacking message for event: {}", user, nackErr);
            }
        }
    }
}
