package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.model.entity.User;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationPublisher {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendUserRegisteredMessage(String user) {
        String exchange = "userExchange";
        String routingKey = "user.registered";
        rabbitTemplate.convertAndSend(exchange, routingKey, user);
    }
}
