package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.model.event.UserEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.example.registrationmodule.constant.Constants.*;

@Service
public class UserPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final String exchange = USER_EXCHANGE;
    private final String userRegisteredRoutingKey = USER_REGISTERED;
    private final String userDeletedRoutingKey = USER_DELETED;

    @Autowired
    public UserPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    }

    public void sendUserRegisteredMessage(UserEvent user) {
        System.out.println("Sending a message " + user + " is added");
        rabbitTemplate.convertAndSend(exchange, userRegisteredRoutingKey, user);
    }

    public void sendUserDeletedMessage(UserEvent user) {
        System.out.println("Sending a message " + user + " is deleted");
        rabbitTemplate.convertAndSend(exchange, userDeletedRoutingKey, user);
    }
}
