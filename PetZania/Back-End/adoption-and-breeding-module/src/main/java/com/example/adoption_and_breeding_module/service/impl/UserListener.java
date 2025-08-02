package com.example.adoption_and_breeding_module.service.impl;

import com.example.adoption_and_breeding_module.model.event.UserEvent;
import com.example.adoption_and_breeding_module.model.entity.User;
import com.example.adoption_and_breeding_module.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Transactional
public class UserListener {
    private final UserRepository userRepository;

    @RabbitListener(queues = "userRegisteredQueueAdoptionModule")
    public void onUserRegistered(UserEvent user) {
        if (!userRepository.existsById(user.getUserId()) && !userRepository.existsByUsername(user.getUsername())
                && !userRepository.existsByEmail(user.getEmail())) {
            User newUser = User.builder()
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .latitude(user.getLatitude() != null ? user.getLatitude() : 0.0)
                    .longitude(user.getLongitude() != null ? user.getLongitude() : 0.0)
                    .build();
            userRepository.save(newUser);
            System.out.println("Received registered user: " + user);
        }
    }

    @RabbitListener(queues = "userDeletedQueueAdoptionModule")
    public void onUserDeleted(UserEvent user) {
        if (userRepository.existsById(user.getUserId())) {
            userRepository.deleteById(user.getUserId());
            System.out.println("Received deleted user: " + user);
        }
    }
}
