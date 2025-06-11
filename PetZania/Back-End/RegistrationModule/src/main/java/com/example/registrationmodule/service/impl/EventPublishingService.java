package com.example.registrationmodule.service.impl;


import com.example.registrationmodule.config.RabbitMQConfig;
import com.example.registrationmodule.model.dto.UserCreatedEventDTO;
import com.example.registrationmodule.model.entity.OutboxEvent;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service responsible for publishing events to message broker
 * Implements Transactional Outbox pattern for reliability
 */
@Service
public class EventPublishingService {
    private static final Logger logger = LoggerFactory.getLogger(EventPublishingService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Publish user created event with retry capability
     *
     * @param event The event to publish
     */
    @Retryable(
            value = {AmqpException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void publishUserCreatedEvent(UserCreatedEventDTO event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.USER_EXCHANGE,
                RabbitMQConfig.USER_CREATED_ROUTING_KEY,
                event
        );
    }

    /**
     * Save failed event to outbox table for later retry
     * Uses a new transaction to ensure it's saved even if the main transaction rolls back
     *
     * @param user The user associated with the event
     * @param eventType The type of event (e.g., USER_CREATED)
     * @param errorMessage The error message explaining the failure
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveForRetry(User user, String eventType, String errorMessage) {
        try {
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setEntityId(user.getUserId().toString());
            outboxEvent.setEntityType("USER");
            outboxEvent.setEventType(eventType);
            outboxEvent.setPayload(objectMapper.writeValueAsString(user));
            outboxEvent.setCreatedAt(LocalDateTime.now());
            outboxEvent.setProcessed(false);
            outboxEvent.setErrorMessage(errorMessage);

            outboxEventRepository.save(outboxEvent);
            logger.info("Event saved to outbox for later retry: {} - {}", eventType, user.getUserId());
        } catch (Exception e) {
            logger.error("Failed to save event to outbox", e);
        }
    }
}
