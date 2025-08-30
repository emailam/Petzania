package com.example.friends.and.chats.module.service.impl;

import com.example.friends.and.chats.module.model.entity.User;
import com.example.friends.and.chats.module.model.event.UserEvent;
import com.example.friends.and.chats.module.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Transactional
@Slf4j
public class UserListener {
    private final UserRepository userRepository;

    @RabbitListener(queues = "userRegisteredQueueFriendsModule", ackMode = "MANUAL")
    public void onUserRegistered(UserEvent user, Channel channel, Message message) {
        try {
            if (!userRepository.existsById(user.getUserId()) && !userRepository.existsByUsername(user.getUsername()) && !userRepository.existsByEmail(user.getEmail())) {
                User newUser = new User();
                newUser.setUserId(user.getUserId());
                newUser.setUsername(user.getUsername());
                newUser.setEmail(user.getEmail());
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

    @RabbitListener(queues = "userDeletedQueueFriendsModule", ackMode = "MANUAL")
    public void onUserDeleted(UserEvent user, Channel channel, Message message) {
        try {
            if (userRepository.existsById(user.getUserId())) {
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
