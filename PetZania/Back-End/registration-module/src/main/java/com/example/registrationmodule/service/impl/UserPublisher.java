package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.model.event.UserEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserPublisher {
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public UserPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    }

    public void sendUserRegisteredMessage(UserEvent user) {
        String exchange = "userExchange";
        String routingKey = "user.registered";
        System.out.println("Sending a message " + user + " is added");
        rabbitTemplate.convertAndSend(exchange, routingKey, user);
    }

    public void sendUserDeletedMessage(UserEvent user) {
        String exchange = "userExchange";
        String routingKey = "user.deleted";
        System.out.println("Sending a message " + user + " is deleted");
        rabbitTemplate.convertAndSend(exchange, routingKey, user);
    }
}
