package com.example.friends.and.chats.module.service.impl;

import com.example.friends.and.chats.module.model.event.FriendEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FriendProducer {

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public FriendProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    }

    public void sendFriendAddedMessage(FriendEvent event) {
        rabbitTemplate.convertAndSend("friendExchange", "friend.added", event);
    }

    public void sendFriendRemovedMessage(FriendEvent event) {
        rabbitTemplate.convertAndSend("friendExchange", "friend.removed", event);
    }
}

