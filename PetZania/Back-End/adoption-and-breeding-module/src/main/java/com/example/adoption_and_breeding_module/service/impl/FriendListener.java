package com.example.adoption_and_breeding_module.service.impl;

import com.example.adoption_and_breeding_module.exception.UserNotFound;
import com.example.adoption_and_breeding_module.model.entity.Friendship;
import com.example.adoption_and_breeding_module.model.entity.User;
import com.example.adoption_and_breeding_module.model.event.FriendEvent;
import com.example.adoption_and_breeding_module.repository.FriendshipRepository;
import com.example.adoption_and_breeding_module.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class FriendListener {
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFound("User not found with ID: " + userId));
    }

    @RabbitListener(queues = "friendAddedQueueAdoptionModule")
    public void onFriendAdded(FriendEvent event) {
        UUID friendshipId = event.getFriendshipId();
        if (!friendshipRepository.existsById(friendshipId)) {
            User user1 = getUser(event.getUser1Id());
            User user2 = getUser(event.getUser2Id());
            Friendship friendship = Friendship.builder()
                    .id(event.getFriendshipId())
                    .user1(user1)
                    .user2(user2)
                    .createdAt(event.getCreatedAt())
                    .build();
            friendshipRepository.save(friendship);
            System.out.println("Friendship added: " + event);
        }
    }

    @RabbitListener(queues = "friendRemovedQueueAdoptionModule")
    public void onFriendRemoved(FriendEvent event) {
        UUID friendshipId = event.getFriendshipId();
        if(friendshipRepository.existsById(friendshipId)) {
            friendshipRepository.deleteById(friendshipId);
            System.out.println("Friendship removed: " + event);
        }
    }
} 