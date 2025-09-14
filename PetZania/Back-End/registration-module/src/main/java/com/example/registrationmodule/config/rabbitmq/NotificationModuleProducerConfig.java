package com.example.registrationmodule.config.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.registrationmodule.constant.Constants.*;

@Configuration
public class NotificationModuleProducerConfig {
    // ===== Queues =====
    @Bean
    public Queue userRegisteredQueueNotificationModule() {
        return QueueBuilder
                .durable(USER_REGISTERED_QUEUE_NOTIFICATION_MODULE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, USER_RETRY_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_REGISTERED_NOTIFICATION_RETRY)
                .build();
    }

    @Bean
    public Queue userRegisteredQueueNotificationModuleRetry() {
        return QueueBuilder
                .durable(USER_REGISTERED_QUEUE_NOTIFICATION_MODULE_RETRY)
                .withArgument(X_MESSAGE_TTL, MESSAGE_TTL_VALUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, USER_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_REGISTERED_NOTIFICATION)
                .build();
    }

    @Bean
    public Queue userDeletedQueueNotificationModule() {
        return QueueBuilder
                .durable(USER_DELETED_QUEUE_NOTIFICATION_MODULE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, USER_RETRY_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_DELETED_NOTIFICATION_RETRY)
                .build();
    }

    @Bean
    public Queue userDeletedQueueNotificationModuleRetry() {
        return QueueBuilder
                .durable(USER_DELETED_QUEUE_NOTIFICATION_MODULE_RETRY)
                .withArgument(X_MESSAGE_TTL, MESSAGE_TTL_VALUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, USER_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_DELETED_NOTIFICATION)
                .build();
    }

    // ===== Bindings =====
    @Bean
    public Binding userRegistrationNotificationModuleBinding(
            Queue userRegisteredQueueNotificationModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userRegisteredQueueNotificationModule)
                .to(userExchange)
                .with(USER_REGISTERED);
    }

    @Bean
    public Binding userDeletionNotificationModuleBinding(
            Queue userDeletedQueueNotificationModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQueueNotificationModule)
                .to(userExchange)
                .with(USER_DELETED);
    }

    @Bean
    public Binding userRegistrationNotificationModuleRetryBinding(
            Queue userRegisteredQueueNotificationModuleRetry, TopicExchange userRetryExchange) {
        return BindingBuilder.bind(userRegisteredQueueNotificationModuleRetry)
                .to(userRetryExchange)
                .with(USER_REGISTERED_NOTIFICATION_RETRY);
    }

    @Bean
    public Binding userDeletionNotificationModuleRetryBinding(
            Queue userDeletedQueueNotificationModuleRetry, TopicExchange userRetryExchange) {
        return BindingBuilder.bind(userDeletedQueueNotificationModuleRetry)
                .to(userRetryExchange)
                .with(USER_DELETED_NOTIFICATION_RETRY);
    }

    @Bean
    public Binding userRegistrationNotificationModuleRetryReturnBinding(
            Queue userRegisteredQueueNotificationModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userRegisteredQueueNotificationModule)
                .to(userExchange)
                .with(USER_REGISTERED_NOTIFICATION);
    }

    @Bean
    public Binding userDeletionNotificationModuleRetryReturnBinding(
            Queue userDeletedQueueNotificationModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQueueNotificationModule)
                .to(userExchange)
                .with(USER_DELETED_NOTIFICATION);
    }
}
