package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.config.RabbitMQConfig;
import com.example.registrationmodule.model.dto.UserCreatedEventDTO;
import com.example.registrationmodule.model.entity.OutboxEvent;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service that retries sending events that previously failed
 */
@Service
public class OutboxProcessorService {
    private static final Logger logger = LoggerFactory.getLogger(OutboxProcessorService.class);

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${application.module.name:FriendsModule}")
    private String moduleName;

    /**
     * Scheduled job to retry sending failed events
     * Runs every 5 minutes
     */
    @Scheduled(fixedDelayString = "${outbox.retry.interval:300000}")
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> events = outboxEventRepository.findEventsForRetry();
        logger.info("Found {} events to retry publishing", events.size());

        for (OutboxEvent event : events) {
            try {
                if ("USER_CREATED".equals(event.getEventType())) {
                    retryUserCreatedEvent(event);
                }
                // Handle other event types as needed

                // Mark as processed
                event.setProcessed(true);
                event.setProcessedAt(LocalDateTime.now());
                outboxEventRepository.save(event);
            } catch (Exception e) {
                logger.error("Failed to process outbox event {}", event.getId(), e);
                event.setRetryCount(event.getRetryCount() + 1);
                event.setErrorMessage(e.getMessage());
                outboxEventRepository.save(event);
            }
        }
    }

    /**
     * Retry sending a user created event
     */
    private void retryUserCreatedEvent(OutboxEvent event) throws Exception {
        // Deserialize payload to User object (simplified, actual implementation may differ)
        User user =
                objectMapper.readValue(event.getPayload(), User.class);

        // Create a new event with unique ID
        UserCreatedEventDTO userEvent = new UserCreatedEventDTO();
        userEvent.setModuleName(moduleName);                       // Source module identifier
        userEvent.setUserId(user.getUserId());
        userEvent.setUsername(user.getUsername());
        userEvent.setEmail(user.getEmail());
        userEvent.setProfilePictureURL(user.getProfilePictureURL());


        // Send to RabbitMQ
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.USER_EXCHANGE,
                RabbitMQConfig.USER_CREATED_ROUTING_KEY,
                userEvent
        );

        logger.info("Successfully retried user created event for user: {}", user.getUserId());
    }

    /**
     * Clean up old processed events
     * Runs once a day
     */
    @Scheduled(cron = "${outbox.cleanup.cron:0 0 0 * * ?}")
    @Transactional
    public void cleanupProcessedEvents() {
        // Delete events that were processed more than 7 days ago
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        List<OutboxEvent> oldEvents = outboxEventRepository.findByProcessedAndProcessedAtBefore(true, cutoffDate);

        if (!oldEvents.isEmpty()) {
            outboxEventRepository.deleteAll(oldEvents);
            logger.info("Deleted {} old processed outbox events", oldEvents.size());
        }
    }
}
