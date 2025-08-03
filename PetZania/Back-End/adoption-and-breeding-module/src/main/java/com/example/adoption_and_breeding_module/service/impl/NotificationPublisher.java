package com.example.adoption_and_breeding_module.service.impl;

import com.example.adoption_and_breeding_module.model.entity.User;
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

    public void sendPetPostLikedNotification(UUID postOwnerId, UUID likerId, UUID postId, String initiatorUsername) {
        String routingKey = "notification.pet_liked";

        NotificationEvent event = NotificationEvent.builder()
                .initiatorId(likerId)
                .recipientId(postOwnerId)
                .entityId(postId)
                .type(NotificationType.PET_POST_LIKED)
                .message(initiatorUsername + " liked your post")
                .build();
        System.out.println("Sending a notification: " + event);
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }

    public void sendPetPostDeleted(UUID postId) {
        String routingKey = "notification.pet_post_deleted";
        NotificationEvent event = NotificationEvent.builder()
                .entityId(postId)
                .type(NotificationType.PET_POST_DELETED)
                .build();
        System.out.println("Sending an event, post with id: " + postId + " is deleted");
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
