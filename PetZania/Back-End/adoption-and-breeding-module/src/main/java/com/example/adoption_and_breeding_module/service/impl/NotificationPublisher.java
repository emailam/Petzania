package com.example.adoption_and_breeding_module.service.impl;

import com.example.adoption_and_breeding_module.model.enumeration.NotificationType;
import com.example.adoption_and_breeding_module.model.event.NotificationEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationPublisher {
    private final RabbitTemplate rabbitTemplate;
    String exchange = "notificationExchange";

    @Autowired
    public NotificationPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    }

    public void sendPetPostLikedNotification(UUID postOwnerId, UUID likerId, UUID postId) {
        String routingKey = "notification.pet_liked";

        Map<String, String> attributes = new HashMap<>();
        attributes.put("senderId", likerId.toString());
        attributes.put("postId", postId.toString());
        attributes.put("postOwnerId", postOwnerId.toString());

        NotificationEvent event = NotificationEvent.builder()
                .recipientId(postOwnerId)
                .type(NotificationType.PET_POST_LIKED)
                .attributes(attributes)
                .message("User Liked Your Post")
                .build();
        System.out.println("Sending a notification: " + event);
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
