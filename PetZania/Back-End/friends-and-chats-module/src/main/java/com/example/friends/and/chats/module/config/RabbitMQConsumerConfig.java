package com.example.friends.and.chats.module.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.friends.and.chats.module.constant.Constants.*;

@Configuration
public class RabbitMQConsumerConfig {
    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(USER_EXCHANGE);
    }

    @Bean
    public TopicExchange userRetryExchange() {
        return new TopicExchange(USER_RETRY_EXCHANGE);
    }

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

    // Retry bindings
    @Bean
    public Binding userRegistrationFriendsModuleRetryBinding(Queue userRegisteredQueueFriendsModuleRetry, TopicExchange userRetryExchange) {
        return BindingBuilder.bind(userRegisteredQueueFriendsModuleRetry).to(userRetryExchange).with(USER_REGISTERED_FRIENDS_RETRY);
    }

    @Bean
    public Binding userDeletionFriendsModuleRetryBinding(Queue userDeletedQueueFriendsModuleRetry, TopicExchange userRetryExchange) {
        return BindingBuilder.bind(userDeletedQueueFriendsModuleRetry).to(userRetryExchange).with(USER_DELETED_FRIENDS_RETRY);
    }

    // Return from retry bindings
    @Bean
    public Binding userRegistrationFriendsModuleRetryReturnBinding(Queue userRegisteredQueueFriendsModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userRegisteredQueueFriendsModule).to(userExchange).with(USER_REGISTERED_FRIENDS);
    }

    @Bean
    public Binding userDeletionFriendsModuleRetryReturnBinding(Queue userDeletedQueueFriendsModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQueueFriendsModule).to(userExchange).with(USER_DELETED_FRIENDS);
    }
}
