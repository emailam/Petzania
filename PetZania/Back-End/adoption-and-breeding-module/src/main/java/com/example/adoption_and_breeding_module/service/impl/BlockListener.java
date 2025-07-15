package com.example.adoption_and_breeding_module.service.impl;

import com.example.adoption_and_breeding_module.exception.UserNotFound;
import com.example.adoption_and_breeding_module.model.entity.Block;
import com.example.adoption_and_breeding_module.model.entity.User;
import com.example.adoption_and_breeding_module.model.event.BlockEvent;
import com.example.adoption_and_breeding_module.repository.BlockRepository;
import com.example.adoption_and_breeding_module.repository.UserRepository;
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

    @RabbitListener(queues = "userBlockedQueueAdoptionModule")
    public void onUserBlocked(BlockEvent blockEvent) {
        return;
        // if(!blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockEvent.getBlockerId(), blockEvent.getBlockedId())) {
        //     User blocker = getUser(blockEvent.getBlockerId());
        //     User blocked = getUser(blockEvent.getBlockedId());
        //     Block block = Block.builder()
        //             .blockId(blockEvent.getBlockId())
        //             .blocker(blocker)
        //             .blocked(blocked)
        //             .createdAt(blockEvent.getCreatedAt())
        //             .build();
        //     blockRepository.save(block);
        //     System.out.println("Received blocked user with IDs:\nBlockerId: " + blockEvent.getBlockerId() + "\nBlockedId: " + blockEvent.getBlockedId());
        // }
    }

    @RabbitListener(queues = "userUnBlockedQueueAdoptionModule")
    public void onUserUnBlocked(BlockEvent blockEvent) {
        if(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockEvent.getBlockerId(), blockEvent.getBlockedId())) {
            blockRepository.deleteByBlocker_UserIdAndBlocked_UserId(blockEvent.getBlockerId(), blockEvent.getBlockedId());
            System.out.println("Received unblocked user with IDs:\nBlockerId: " + blockEvent.getBlockerId() + "\nBlockedId: " + blockEvent.getBlockedId());
        }
    }
}
