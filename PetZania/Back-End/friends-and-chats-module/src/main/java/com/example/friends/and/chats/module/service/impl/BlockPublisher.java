package com.example.friends.and.chats.module.service.impl;

import com.example.friends.and.chats.module.model.event.BlockEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.example.friends.and.chats.module.constant.Constants.*;

@Service
public class BlockPublisher {
    private final RabbitTemplate rabbitTemplate;
    private static final String exchange = BLOCK_EXCHANGE;
    private static final String routingKeyBlockAdd = BLOCK_ADD;
    private static final String routingKeyBlockDelete = BLOCK_DELETE;

    @Autowired
    public BlockPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    }

    public void sendUserBlockedMessage(BlockEvent blockEvent) {
        System.out.println("Sending a message: User " + blockEvent.getBlockedId() + " is blocked by User " + blockEvent.getBlockerId());
        rabbitTemplate.convertAndSend(exchange, routingKeyBlockAdd, blockEvent);
    }

    public void sendUserUnBlockedMessage(BlockEvent blockEvent) {
        System.out.println("Sending a message: User " + blockEvent.getBlockedId() + " is unblocked by User " + blockEvent.getBlockerId());
        rabbitTemplate.convertAndSend(exchange, routingKeyBlockDelete, blockEvent);
    }
}
