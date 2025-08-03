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

    public void sendFriendRequestNotification(UUID senderId, UUID receiverId, UUID requestId, String senderUsername) {
        String routingKey = "notification.friend_request_received";

        NotificationEvent event = NotificationEvent.builder()
                .initiatorId(senderId)
                .recipientId(receiverId)
                .entityId(requestId)
                .type(NotificationType.FRIEND_REQUEST_RECEIVED)
                .message(senderUsername + " sent you a friend request")
                .build();

        System.out.println("Sent friend request notification from " + senderId + " to recipient: " + receiverId);
        System.out.println("Event: " + event);
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }

    public void sendFriendRequestAcceptedNotification(UUID senderId, UUID receiverId, UUID friendshipId, String receiverUsername) {
        String routingKey = "notification.friend_request_accepted";

        NotificationEvent event = NotificationEvent.builder()
                .initiatorId(receiverId)
                .recipientId(senderId)
                .entityId(friendshipId)
                .type(NotificationType.FRIEND_REQUEST_ACCEPTED)
                .message(receiverUsername + " accepted your friend request")
                .build();


        System.out.println("Sent friend request accepted notification from " + receiverId + " to recipient: " + senderId);
        System.out.println("Event: " + event);
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }

    public void sendNewFollowerNotification(UUID followerId, UUID followedId, UUID followId, String followerUsername) {
        String routingKey = "notification.new_follower";

        NotificationEvent event = NotificationEvent.builder()
                .initiatorId(followerId)
                .recipientId(followedId)
                .entityId(followId)
                .type(NotificationType.NEW_FOLLOWER)
                .message(followerUsername + " started following you")
                .build();

        System.out.println("Sent new follower notification from " + followerId + " to recipient: " + followedId);
        System.out.println("Event: " + event);
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }

    public void sendFriendRequestCancelled(UUID friendRequestId) {
        String routingKey = "notification.friend_request_cancelled";

        NotificationEvent event = NotificationEvent.builder()
                .entityId(friendRequestId)
                .type(NotificationType.FRIEND_REQUEST_WITHDRAWN)
                .build();

        System.out.println("Friend request cancelled " + friendRequestId);
        System.out.println("Event: " + event);
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
