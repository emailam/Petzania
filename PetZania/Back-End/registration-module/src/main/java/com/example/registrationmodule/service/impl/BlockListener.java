package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.exception.user.UserNotFound;
import com.example.registrationmodule.model.entity.Block;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.model.event.BlockEvent;
import com.example.registrationmodule.repository.BlockRepository;
import com.example.registrationmodule.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class BlockListener {
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;

    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFound("User not found with ID: " + userId));
    }

    @RabbitListener(queues = "userBlockedQueueRegistrationModule")
    public void onUserBlocked(BlockEvent blockEvent) {
        if (!blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockEvent.getBlockerId(), blockEvent.getBlockedId())) {
            User blocker = getUser(blockEvent.getBlockerId());
            User blocked = getUser(blockEvent.getBlockedId());
            Block block = new Block();
            block.setBlockId(blockEvent.getBlockId());
            block.setBlocker(blocker);
            block.setBlocked(blocked);
            block.setCreatedAt(blockEvent.getCreatedAt());
            blockRepository.save(block);
            System.out.println("Received blocked user with IDs:\nBlockerId: " + blockEvent.getBlockerId() + "\nBlockedId: " + blockEvent.getBlockedId());
        }
    }

    @RabbitListener(queues = "userUnBlockedQueueRegistrationModule")
    public void onUserUnBlocked(BlockEvent blockEvent) {
        if (blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockEvent.getBlockerId(), blockEvent.getBlockedId())) {
            blockRepository.deleteByBlocker_UserIdAndBlocked_UserId(blockEvent.getBlockerId(), blockEvent.getBlockedId());
            System.out.println("Received unblocked user with IDs:\nBlockerId: " + blockEvent.getBlockerId() + "\nBlockedId: " + blockEvent.getBlockedId());
        }
    }
}