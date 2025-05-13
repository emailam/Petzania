package com.example.friends.and.chats.module.service.impl;

import com.example.friends.and.chats.module.config.RabbitMQConfig;
import com.example.friends.and.chats.module.model.dto.UserCreatedEventDTO;
import com.example.friends.and.chats.module.model.entity.User;
import com.example.friends.and.chats.module.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserCreatedListener {
    @Value("${spring.application.name}")
    private String moduleName;

    private static final Logger logger = LoggerFactory.getLogger(UserCreatedListener.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventTrackingService eventTrackingService;

    @Autowired
    private DeadLetterService deadLetterService;

    /**
     * Handle user created event from RabbitMQ with more forgiving validation
     */
    @Transactional
    @RabbitListener(queues = RabbitMQConfig.USER_CREATED_QUEUE)
    public void handleUserCreated(UserCreatedEventDTO event) {
        try {
            processUserCreatedEvent(event);
        } catch (AmqpRejectAndDontRequeueException e) {
            logger.warn("Message rejected and not requeued: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error processing user creation event: {}", event != null ? event.getUserId() : "unknown", e);
            deadLetterService.sendToDeadLetter(event, e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Failed to process message, sent to DLQ", e);
        }
    }

    /**
     * Process the user created event with more lenient validation
     * This allows for backward compatibility with messages that might not have eventId
     */
    private void processUserCreatedEvent(UserCreatedEventDTO event) {
        // Basic validation - note we don't strictly require eventId anymore
        if (event == null || event.getUserId() == null || event.getEmail() == null) {
            logger.warn("Invalid event received: {}", event);
            throw new AmqpRejectAndDontRequeueException("Invalid event data");
        }

        // Skip events from our own module
        if (moduleName.equals(event.getModuleName())) {
            logger.debug("Skipping event from own module: {}", event.getEventId());
            return;
        }

        // For events without an eventId, generate one based on userId to ensure idempotency
        String effectiveEventId = event.getEventId() != null ?
                event.getEventId().toString() :
                "generated-" + event.getUserId().toString();

        // Check if the event has been processed already
        if (eventTrackingService.isEventProcessed(effectiveEventId)) {
            logger.debug("Event already processed: {}", effectiveEventId);
            return;
        }

        // Mark the event as processed
        eventTrackingService.markEventAsProcessed(effectiveEventId);

        try {
            // Check if the email already exists
            Optional<User> existingUserOpt = userRepository.findByEmailIgnoreCase(event.getEmail());

            if (!existingUserOpt.isPresent()) {
                // Create a new user
                User user = new User();
                user.setUserId(event.getUserId());
                user.setUsername(event.getUsername());
                user.setEmail(event.getEmail());
                user.setProfilePictureURL(event.getProfilePictureURL());
                userRepository.save(user);
                logger.info("Created new user with ID: {}", event.getUserId());
            } else {
                // Update existing user
                User existingUser = existingUserOpt.get();
                existingUser.setProfilePictureURL(event.getProfilePictureURL());
                existingUser.setUsername(event.getUsername());
                userRepository.save(existingUser);
                logger.info("Updated existing user with ID: {}", existingUser.getUserId());
            }
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation when saving user: {}", event.getUserId(), e);
            eventTrackingService.unmarkEvent(effectiveEventId);
            throw new AmqpRejectAndDontRequeueException("Database constraint violation", e);
        }
    }
}