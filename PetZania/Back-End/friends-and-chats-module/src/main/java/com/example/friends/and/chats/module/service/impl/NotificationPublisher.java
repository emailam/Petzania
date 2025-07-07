package com.example.friends.and.chats.module.service.impl;

import com.example.friends.and.chats.module.model.enumeration.NotificationType;
import com.example.friends.and.chats.module.model.event.NotificationEvent;
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

    public void sendFriendRequestNotification(UUID senderId, UUID receiverId, UUID requestId){
        String routingKey = "notification.friend_request_received";

        Map<String, String> attributes = new HashMap<>();
        attributes.put("senderId", senderId.toString());
        attributes.put("receiverId", receiverId.toString());
        attributes.put("requestId", requestId.toString());

        NotificationEvent event = NotificationEvent.builder()
                .initiatorId(senderId)
                .recipientId(receiverId)
                .type(NotificationType.FRIEND_REQUEST_RECEIVED)
                .attributes(attributes)
                .message("You Have A New Friend Request")
                .build();

        System.out.println("Sent friend request notification from " + senderId + " to recipient: " + receiverId);
        System.out.println("Event: " + event);
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }

    public void sendFriendRequestAcceptedNotification(UUID senderId, UUID receiverId) {
        String routingKey = "notification.friend_request_accepted";


        Map<String, String> attributes = new HashMap<>();
        attributes.put("senderId", senderId.toString());
        attributes.put("receiverId", receiverId.toString());

        NotificationEvent event = NotificationEvent.builder()
                .initiatorId(receiverId)
                .recipientId(senderId)
                .type(NotificationType.FRIEND_REQUEST_ACCEPTED)
                .attributes(attributes)
                .message("User Accepted Your Friend Request")
                .build();


        System.out.println("Sent friend request accepted notification from " + receiverId + " to recipient: " + senderId);
        System.out.println("Event: " + event);
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }

    public void sendNewFollowerNotification(UUID followerId, UUID followedId) {
        String routingKey = "notification.new_follower";

        Map<String, String> attributes = new HashMap<>();
        attributes.put("senderId", followerId.toString());
        attributes.put("receiverId", followedId.toString());

        NotificationEvent event = NotificationEvent.builder()
                .initiatorId(followerId)
                .recipientId(followedId)
                .type(NotificationType.NEW_FOLLOWER)
                .attributes(attributes)
                .message("User Started Following You")
                .build();

        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        System.out.println("Sent new follower notification from " + followerId + " to recipient: " + followedId);
        System.out.println("Event: " + event);
    }
}
