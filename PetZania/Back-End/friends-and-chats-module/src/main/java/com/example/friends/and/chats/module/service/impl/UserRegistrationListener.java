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
public class UserRegistrationListener {
    private final UserRepository userRepository;
    private static int cnt = 1;

    @RabbitListener(queues = "userQueue")
    public void onMessage(UserEvent user) {
        User newUser = new User();
        newUser.setUserId(user.getUserId());
        newUser.setUsername(user.getUsername() + cnt);
        newUser.setEmail(user.getEmail() + cnt);
        userRepository.save(newUser);
        cnt++;
        System.out.println("received a message: " + user);
    }
}
