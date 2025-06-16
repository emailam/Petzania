package com.example.friends.and.chats.module.service.impl;

import com.example.friends.and.chats.module.model.event.BlockEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BlockPublisher {
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public BlockPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    }

    public void sendUserBlockedMessage(BlockEvent blockEvent) {
        String exchange = "blockExchange";
        String routingKey = "block.add";
        System.out.println("Sending a message: User " + blockEvent.getBlockedId() + " is blocked by User " + blockEvent.getBlockerId());
        rabbitTemplate.convertAndSend(exchange, routingKey, blockEvent);
    }

    public void sendUserUnBlockedMessage(BlockEvent blockEvent) {
        String exchange = "blockExchange";
        String routingKey = "block.delete";
        System.out.println("Sending a message: User " + blockEvent.getBlockedId() + " is unblocked by User " + blockEvent.getBlockerId());
        rabbitTemplate.convertAndSend(exchange, routingKey, blockEvent);
    }
}
