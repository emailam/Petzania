package com.example.friends.and.chats.module.service.impl;

import com.example.friends.and.chats.module.model.event.UserEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationListener {

    @RabbitListener(queues = "userQueue")
    public void onMessage(UserEvent user) {
        System.out.println("received a message: " + user);
    }
}
