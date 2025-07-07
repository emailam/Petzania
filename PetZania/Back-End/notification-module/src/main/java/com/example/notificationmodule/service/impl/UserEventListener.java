package com.example.notificationmodule.service.impl;

import com.example.notificationmodule.model.entity.User;
import com.example.notificationmodule.model.event.UserEvent;
import com.example.notificationmodule.repository.NotificationRepository;
import com.example.notificationmodule.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Transactional
public class UserEventListener {
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @RabbitListener(queues = "userRegisteredQueueNotificationModule")
    public void onUserRegistered(UserEvent user) {
        if (!userRepository.existsById(user.getUserId()) && !userRepository.existsByUsername(user.getUsername()) && !userRepository.existsByEmail(user.getEmail())) {
            User newUser = new User();
            newUser.setUserId(user.getUserId());
            newUser.setUsername(user.getUsername());
            newUser.setEmail(user.getEmail());
            userRepository.save(newUser);
            System.out.println("received registered user: " + user);
        }
    }

    @RabbitListener(queues = "userDeletedQueueNotificationModule")
    public void onUserDeleted(UserEvent user) {
        if (userRepository.existsById(user.getUserId())) {
            userRepository.deleteById(user.getUserId());
            notificationRepository.deleteByRecipientId(user.getUserId());
            notificationRepository.deleteByInitiatorId(user.getUserId());
            System.out.println("received deleted user: " + user);
        }
    }
}
