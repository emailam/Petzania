package com.example.registrationmodule.config.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.registrationmodule.constant.Constants.*;


@Configuration
public class FriendsModuleProducerConfig {
    @Bean
    public Queue userRegisteredQueueFriendsModule() {
        return QueueBuilder
                .durable(USER_REGISTERED_QUEUE_FRIENDS_MODULE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, USER_RETRY_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_REGISTERED_FRIENDS_RETRY)
                .build();
    }

    @Bean
    public Queue userRegisteredQueueFriendsModuleRetry() {
        return QueueBuilder
                .durable(USER_REGISTERED_QUEUE_FRIENDS_MODULE_RETRY)
                .withArgument(X_MESSAGE_TTL, MESSAGE_TTL_VALUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, USER_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_REGISTERED_FRIENDS)
                .build();
    }

    @Bean
    public Queue userDeletedQueueFriendsModule() {
        return QueueBuilder
                .durable(USER_DELETED_QUEUE_FRIENDS_MODULE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, USER_RETRY_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_DELETED_FRIENDS_RETRY)
                .build();
    }

    @Bean
    public Queue userDeletedQueueFriendsModuleRetry() {
        return QueueBuilder
                .durable(USER_DELETED_QUEUE_FRIENDS_MODULE_RETRY)
                .withArgument(X_MESSAGE_TTL, MESSAGE_TTL_VALUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, USER_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_DELETED_FRIENDS)
                .build();
    }

    @Bean
    public Binding userRegistrationFriendsModuleBinding(Queue userRegisteredQueueFriendsModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userRegisteredQueueFriendsModule).to(userExchange).with(USER_REGISTERED);
    }

    @Bean
    public Binding userDeletionFriendsModuleBinding(Queue userDeletedQueueFriendsModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQueueFriendsModule).to(userExchange).with(USER_DELETED);
    }

    @Bean
    public Binding userRegistrationFriendsModuleRetryBinding(Queue userRegisteredQueueFriendsModuleRetry, TopicExchange userRetryExchange) {
        return BindingBuilder.bind(userRegisteredQueueFriendsModuleRetry).to(userRetryExchange).with(USER_REGISTERED_FRIENDS_RETRY);
    }

    @Bean
    public Binding userDeletionFriendsModuleRetryBinding(Queue userDeletedQueueFriendsModuleRetry, TopicExchange userRetryExchange) {
        return BindingBuilder.bind(userDeletedQueueFriendsModuleRetry).to(userRetryExchange).with(USER_DELETED_FRIENDS_RETRY);
    }

    @Bean
    public Binding userRegistrationFriendsModuleRetryReturnBinding(Queue userRegisteredQueueFriendsModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userRegisteredQueueFriendsModule).to(userExchange).with(USER_REGISTERED_FRIENDS);
    }

    @Bean
    public Binding userDeletionFriendsModuleRetryReturnBinding(Queue userDeletedQueueFriendsModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQueueFriendsModule).to(userExchange).with(USER_DELETED_FRIENDS);
    }
}
