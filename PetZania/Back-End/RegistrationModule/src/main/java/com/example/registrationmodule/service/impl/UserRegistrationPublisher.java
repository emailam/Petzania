package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.model.event.UserEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationPublisher {
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public UserRegistrationPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    }
    public void sendUserRegisteredMessage(UserEvent user) {
        String exchange = "userExchange";
        String routingKey = "user.registered";
        System.out.println("Sending a message " + user);
        rabbitTemplate.convertAndSend(exchange, routingKey, user);
    }
}
