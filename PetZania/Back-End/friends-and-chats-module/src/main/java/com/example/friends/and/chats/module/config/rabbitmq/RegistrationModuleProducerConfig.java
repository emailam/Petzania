package com.example.friends.and.chats.module.config.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.friends.and.chats.module.constant.Constants.*;

@Configuration
public class RegistrationModuleProducerConfig {
    @Bean
    public Queue userBlockedQueueRegistrationModule() {
        return QueueBuilder
                .durable(USER_BLOCKED_QUEUE_REGISTRATION_MODULE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, BLOCK_RETRY_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_BLOCKED_REGISTRATION_RETRY)
                .build();
    }

    @Bean
    public Queue userBlockedQueueRegistrationModuleRetry() {
        return QueueBuilder
                .durable(USER_BLOCKED_QUEUE_REGISTRATION_MODULE_RETRY)
                .withArgument(X_MESSAGE_TTL, MESSAGE_TTL_VALUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, BLOCK_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_BLOCKED_REGISTRATION)
                .build();
    }

    @Bean
    public Queue userUnBlockedQueueRegistrationModule() {
        return QueueBuilder
                .durable(USER_UNBLOCKED_QUEUE_REGISTRATION_MODULE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, BLOCK_RETRY_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_UNBLOCKED_REGISTRATION_RETRY)
                .build();
    }

    @Bean
    public Queue userUnBlockedQueueRegistrationModuleRetry() {
        return QueueBuilder
                .durable(USER_UNBLOCKED_QUEUE_REGISTRATION_MODULE_RETRY)
                .withArgument(X_MESSAGE_TTL, MESSAGE_TTL_VALUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, BLOCK_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_UNBLOCKED_REGISTRATION)
                .build();
    }

    @Bean
    public Binding userBlockingRegistrationBinding(Queue userBlockedQueueRegistrationModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userBlockedQueueRegistrationModule).to(blockExchange).with(BLOCK_ADD);
    }


    @Bean
    public Binding userUnBlockingRegistrationBinding(Queue userUnBlockedQueueRegistrationModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userUnBlockedQueueRegistrationModule).to(blockExchange).with(BLOCK_DELETE);
    }

    @Bean
    public Binding userBlockingRegistrationRetryBinding(Queue userBlockedQueueRegistrationModuleRetry, TopicExchange blockRetryExchange) {
        return BindingBuilder.bind(userBlockedQueueRegistrationModuleRetry).to(blockRetryExchange).with(USER_BLOCKED_REGISTRATION_RETRY);
    }

    @Bean
    public Binding userUnBlockingRegistrationRetryBinding(Queue userUnBlockedQueueRegistrationModuleRetry, TopicExchange blockRetryExchange) {
        return BindingBuilder.bind(userUnBlockedQueueRegistrationModuleRetry).to(blockRetryExchange).with(USER_UNBLOCKED_REGISTRATION_RETRY);
    }

    // Return from retry bindings
    @Bean
    public Binding userBlockingRegistrationRetryReturnBinding(Queue userBlockedQueueRegistrationModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userBlockedQueueRegistrationModule).to(blockExchange).with(USER_BLOCKED_REGISTRATION);
    }

    @Bean
    public Binding userUnBlockingRegistrationRetryReturnBinding(Queue userUnBlockedQueueRegistrationModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userUnBlockedQueueRegistrationModule).to(blockExchange).with(USER_UNBLOCKED_REGISTRATION);
    }
}
