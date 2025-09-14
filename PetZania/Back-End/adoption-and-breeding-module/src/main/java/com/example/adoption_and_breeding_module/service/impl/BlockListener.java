package com.example.adoption_and_breeding_module.service.impl;

import com.example.adoption_and_breeding_module.exception.UserNotFound;
import com.example.adoption_and_breeding_module.model.entity.Block;
import com.example.adoption_and_breeding_module.model.entity.User;
import com.example.adoption_and_breeding_module.model.event.BlockEvent;
import com.example.adoption_and_breeding_module.repository.BlockRepository;
import com.example.adoption_and_breeding_module.repository.UserRepository;
import com.example.adoption_and_breeding_module.util.QueueUtils;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import lombok.extern.slf4j.Slf4j;

import static com.example.adoption_and_breeding_module.constant.Constants.*;

import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
@Slf4j
public class BlockListener {
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final QueueUtils queueUtils;

    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFound("User not found with ID: " + userId));
    }

    @RabbitListener(queues = USER_BLOCKED_QUEUE_ADOPTION_MODULE, ackMode = ACK_MODE)
    public void onUserBlocked(BlockEvent blockEvent, Channel channel, Message message) {
        try {
            if (!blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockEvent.getBlockerId(), blockEvent.getBlockedId())) {
                User blocker = getUser(blockEvent.getBlockerId());
                User blocked = getUser(blockEvent.getBlockedId());
                Block block = Block.builder()
                        .blockId(blockEvent.getBlockId())
                        .blocker(blocker)
                        .blocked(blocked)
                        .createdAt(blockEvent.getCreatedAt())
                        .build();
                blockRepository.save(block);
                log.info("Received blocked user with IDs: BlockerId: {} BlockedId: {}", blockEvent.getBlockerId(), blockEvent.getBlockedId());
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception ex) {
            log.error("Error processing user blocked event: {}", blockEvent, ex);
            try {
                int retryCount = queueUtils.getRetryCount(message, USER_BLOCKED_QUEUE_ADOPTION_MODULE_RETRY);
                if (retryCount >= MAX_RETRIES) {
                    // simply drop the message
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    log.info("Max retries reached for the event: {}", blockEvent);
                } else {
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
                    log.info("This is the retry number: {} for event: {}", retryCount, blockEvent);
                }
            } catch (Exception nackErr) {
                log.error("Error nacking message for event: {}", blockEvent, nackErr);
            }
        }
    }

    @RabbitListener(queues = USER_UNBLOCKED_QUEUE_ADOPTION_MODULE, ackMode = ACK_MODE)
    public void onUserUnBlocked(BlockEvent blockEvent, Channel channel, Message message) {
        try {
            if (blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockEvent.getBlockerId(), blockEvent.getBlockedId())) {
                blockRepository.deleteByBlocker_UserIdAndBlocked_UserId(blockEvent.getBlockerId(), blockEvent.getBlockedId());
                log.info("Received unblocked user with IDs: BlockerId: {} BlockedId: {}", blockEvent.getBlockerId(), blockEvent.getBlockedId());
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception ex) {
            log.error("Error processing user unblocked event: {}", blockEvent, ex);
            try {
                int retryCount = queueUtils.getRetryCount(message, USER_UNBLOCKED_QUEUE_ADOPTION_MODULE_RETRY);
                if (retryCount >= MAX_RETRIES) {
                    // simply drop the message
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    log.info("Max retries reached for the event: {}", blockEvent);
                } else {
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
                    log.info("This is the retry number: {} for event: {}", retryCount, blockEvent);
                }
            } catch (Exception nackErr) {
                log.error("Error nacking message for event: {}", blockEvent, nackErr);
            }
        }

    }
}
