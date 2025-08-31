package com.example.friends.and.chats.module.service.impl;

import com.example.friends.and.chats.module.model.enumeration.NotificationType;
import com.example.friends.and.chats.module.model.event.NotificationEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.example.friends.and.chats.module.constant.Constants.*;

@Service
public class NotificationPublisher {
    private final RabbitTemplate rabbitTemplate;
    private static final String exchange = NOTIFICATION_EXCHANGE;
    private static final String friendRequestReceivedRoutingKey = NOTIFICATION_FRIEND_REQUEST_RECEIVED;
    private static final String friendRequestAcceptedRoutingKey = NOTIFICATION_FRIEND_REQUEST_ACCEPTED;
    private static final String newFollowerRoutingKey = NOTIFICATION_NEW_FOLLOWER;
    private static final String friendRequestCancelledRoutingKey = NOTIFICATION_FRIEND_REQUEST_CANCELLED;

    @Autowired
    public NotificationPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    }

    public void sendFriendRequestNotification(UUID senderId, UUID receiverId, UUID requestId, String senderUsername) {
        NotificationEvent event = NotificationEvent.builder()
                .initiatorId(senderId)
                .recipientId(receiverId)
                .entityId(requestId)
                .type(NotificationType.FRIEND_REQUEST_RECEIVED)
                .message(senderUsername + " sent you a friend request")
                .build();

        System.out.println("Sent friend request notification from " + senderId + " to recipient: " + receiverId);
        System.out.println("Event: " + event);
        rabbitTemplate.convertAndSend(exchange, friendRequestReceivedRoutingKey, event);
    }

    public void sendFriendRequestAcceptedNotification(UUID senderId, UUID receiverId, UUID friendshipId, String receiverUsername) {
        NotificationEvent event = NotificationEvent.builder()
                .initiatorId(receiverId)
                .recipientId(senderId)
                .entityId(friendshipId)
                .type(NotificationType.FRIEND_REQUEST_ACCEPTED)
                .message(receiverUsername + " accepted your friend request")
                .build();


        System.out.println("Sent friend request accepted notification from " + receiverId + " to recipient: " + senderId);
        System.out.println("Event: " + event);
        rabbitTemplate.convertAndSend(exchange, friendRequestAcceptedRoutingKey, event);
    }

    public void sendNewFollowerNotification(UUID followerId, UUID followedId, UUID followId, String followerUsername) {
        NotificationEvent event = NotificationEvent.builder()
                .initiatorId(followerId)
                .recipientId(followedId)
                .entityId(followId)
                .type(NotificationType.NEW_FOLLOWER)
                .message(followerUsername + " started following you")
                .build();

        System.out.println("Sent new follower notification from " + followerId + " to recipient: " + followedId);
        System.out.println("Event: " + event);
        rabbitTemplate.convertAndSend(exchange, newFollowerRoutingKey, event);
    }

    public void sendFriendRequestCancelled(UUID friendRequestId) {
        NotificationEvent event = NotificationEvent.builder()
                .entityId(friendRequestId)
                .type(NotificationType.FRIEND_REQUEST_WITHDRAWN)
                .build();

        System.out.println("Friend request cancelled " + friendRequestId);
        System.out.println("Event: " + event);
        rabbitTemplate.convertAndSend(exchange, friendRequestCancelledRoutingKey, event);
    }
}
