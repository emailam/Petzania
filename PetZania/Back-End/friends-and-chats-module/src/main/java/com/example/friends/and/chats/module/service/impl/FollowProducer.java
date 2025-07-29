package com.example.friends.and.chats.module.service.impl;

import com.example.friends.and.chats.module.model.event.FollowEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FollowProducer {

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public FollowProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    }

    public void sendFollowAddedMessage(FollowEvent event) {
        rabbitTemplate.convertAndSend("followExchange", "follow.added", event);
    }

    public void sendFollowRemovedMessage(FollowEvent event) {
        rabbitTemplate.convertAndSend("followExchange", "follow.removed", event);
    }
}
