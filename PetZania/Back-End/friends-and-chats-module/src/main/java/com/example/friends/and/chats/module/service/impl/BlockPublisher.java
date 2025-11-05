package com.example.friends.and.chats.module.service.impl;

import com.example.friends.and.chats.module.model.event.BlockEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.example.friends.and.chats.module.constant.Constants.*;

@Service
@Slf4j
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
        log.info("Sending a message: User {} is blocked by User {}", blockEvent.getBlockedId(), blockEvent.getBlockerId());
        rabbitTemplate.convertAndSend(exchange, routingKeyBlockAdd, blockEvent);
    }

    public void sendUserUnBlockedMessage(BlockEvent blockEvent) {
        log.info("Sending a message: User {} is unblocked by User {}", blockEvent.getBlockedId(), blockEvent.getBlockerId());
        rabbitTemplate.convertAndSend(exchange, routingKeyBlockDelete, blockEvent);
    }
}
