package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.exception.user.UserNotFound;
import com.example.registrationmodule.model.entity.Block;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.model.event.BlockEvent;
import com.example.registrationmodule.repository.BlockRepository;
import com.example.registrationmodule.repository.UserRepository;
import com.example.registrationmodule.util.QueueUtils;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import lombok.extern.slf4j.Slf4j;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.example.registrationmodule.constant.Constants.*;

@Service
@AllArgsConstructor
@Transactional
@Slf4j
public class BlockListener {
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final QueueUtils queueUtils;

    public User getUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFound("User not found with ID: " + userId));
    }

    @RabbitListener(queues = USER_BLOCKED_QUEUE_REGISTRATION_MODULE, ackMode = ACK_MODE)
    public void onUserBlocked(BlockEvent blockEvent, Channel channel, Message message) {
        try {
            if (!blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockEvent.getBlockerId(), blockEvent.getBlockedId())) {
                User blocker = getUser(blockEvent.getBlockerId());
                User blocked = getUser(blockEvent.getBlockedId());
                Block block = new Block();
                block.setBlockId(blockEvent.getBlockId());
                block.setBlocker(blocker);
                block.setBlocked(blocked);
                block.setCreatedAt(blockEvent.getCreatedAt());
                blockRepository.save(block);
                log.info("Received blocked user with IDs: BlockerId: {} BlockedId: {}", blockEvent.getBlockerId(), blockEvent.getBlockedId());
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception ex) {
            // If you want to requeue the message you will have to change the third parameter (b1) to true.
            log.error("Error processing block event: {}", blockEvent, ex);
            try {
                int retryCount = queueUtils.getRetryCount(message, USER_BLOCKED_QUEUE_REGISTRATION_MODULE_RETRY);
                if (retryCount >= MAX_RETRIES) {
                    // simply drop the message
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    log.info("Max retries reached for the event: {}", blockEvent);
                } else {
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
                    log.info("This is the retry number: {} for event: {}", retryCount, blockEvent);
                }
            } catch (Exception nackErr) {
                // Maybe connection to the queue is lost
                log.error("Error nacking message for event: {}", blockEvent, nackErr);
            }
        }
    }

    @RabbitListener(queues = USER_UNBLOCKED_QUEUE_REGISTRATION_MODULE, ackMode = ACK_MODE)
    public void onUserUnBlocked(BlockEvent blockEvent, Channel channel, Message message) {
        try {
            if (blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockEvent.getBlockerId(), blockEvent.getBlockedId())) {
                blockRepository.deleteByBlocker_UserIdAndBlocked_UserId(blockEvent.getBlockerId(), blockEvent.getBlockedId());
                log.info("Received unblocked user with IDs: BlockerId: {} BlockedId: {}", blockEvent.getBlockerId(), blockEvent.getBlockedId());
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception ex) {
            log.error("Error processing unblock event: {}", blockEvent, ex);
            try {
                int retryCount = queueUtils.getRetryCount(message, USER_UNBLOCKED_QUEUE_REGISTRATION_MODULE_RETRY);
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