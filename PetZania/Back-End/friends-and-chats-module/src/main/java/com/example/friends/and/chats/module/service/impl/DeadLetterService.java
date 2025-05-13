package com.example.friends.and.chats.module.service.impl;

import com.example.friends.and.chats.module.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for handling messages that fail processing and need to be sent to a dead letter queue
 */
@Service
public class DeadLetterService {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Send a failed message to the dead letter queue
     *
     * @param message The original message that failed processing
     * @param errorMessage Description of the error that occurred
     */
    public void sendToDeadLetter(Object message, String errorMessage) {
        try {
            // Create a wrapper that includes both the original message and the error
            DeadLetterMessage deadLetterMessage = new DeadLetterMessage(message, errorMessage);

            // Send to the dead letter exchange
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DEAD_LETTER_EXCHANGE,
                    RabbitMQConfig.DEAD_LETTER_ROUTING_KEY,
                    deadLetterMessage
            );

            logger.info("Message sent to dead letter queue: {}", errorMessage);
        } catch (Exception e) {
            // If even sending to DLQ fails, log it but don't throw (to prevent infinite loops)
            logger.error("Failed to send message to dead letter queue", e);
        }
    }

    /**
     * Wrapper class for dead letter messages that includes the original message and error information
     */
    public static class DeadLetterMessage {
        private final Object originalMessage;
        private final String errorMessage;
        private final long timestamp;

        public DeadLetterMessage(Object originalMessage, String errorMessage) {
            this.originalMessage = originalMessage;
            this.errorMessage = errorMessage;
            this.timestamp = System.currentTimeMillis();
        }

        public Object getOriginalMessage() {
            return originalMessage;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}