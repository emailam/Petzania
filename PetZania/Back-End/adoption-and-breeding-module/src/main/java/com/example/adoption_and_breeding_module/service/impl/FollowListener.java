package com.example.adoption_and_breeding_module.service.impl;

import com.example.adoption_and_breeding_module.exception.UserNotFound;
import com.example.adoption_and_breeding_module.model.entity.Follow;
import com.example.adoption_and_breeding_module.model.entity.User;
import com.example.adoption_and_breeding_module.model.event.FollowEvent;
import com.example.adoption_and_breeding_module.repository.FollowRepository;
import com.example.adoption_and_breeding_module.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class FollowListener {
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFound("User not found with ID: " + userId));
    }

    @RabbitListener(queues = "followAddedQueueAdoptionModule")
    public void onFollowAdded(FollowEvent event) {
        UUID followId = event.getFollowId();
        if (!followRepository.existsById(followId)) {
            User follower = getUser(event.getFollowerId());
            User followed = getUser(event.getFollowedId());
            Follow follow = Follow.builder()
                    .id(event.getFollowId())
                    .follower(follower)
                    .followed(followed) 
                    .createdAt(event.getCreatedAt())
                    .build();
            followRepository.save(follow);
            System.out.println("Follow added: " + event);
        }
    }

    @RabbitListener(queues = "followRemovedQueueAdoptionModule")
    public void onFollowRemoved(FollowEvent event) {
        UUID followId = event.getFollowId();
        if(followRepository.existsById(followId)) {
            followRepository.deleteById(followId);
            System.out.println("Follow removed: " + event);
        }
    }
} 