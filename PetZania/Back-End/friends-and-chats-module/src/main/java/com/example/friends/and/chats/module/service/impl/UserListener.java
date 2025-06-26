package com.example.friends.and.chats.module.service.impl;

import com.example.friends.and.chats.module.model.entity.User;
import com.example.friends.and.chats.module.model.event.UserEvent;
import com.example.friends.and.chats.module.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Transactional
public class UserListener {
    private final UserRepository userRepository;

    @RabbitListener(queues = "userRegisteredQueueFriendsModule")
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

    @RabbitListener(queues = "userDeletedQueueFriendsModule")
    public void onUserDeleted(UserEvent user) {
        if (userRepository.existsById(user.getUserId())) {
            userRepository.deleteById(user.getUserId());
            System.out.println("received deleted user: " + user);
        }
    }
}
