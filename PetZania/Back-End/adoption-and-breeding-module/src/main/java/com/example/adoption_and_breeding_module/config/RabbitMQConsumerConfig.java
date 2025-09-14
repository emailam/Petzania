package com.example.adoption_and_breeding_module.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.adoption_and_breeding_module.constant.Constants.*;

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
    public TopicExchange blockExchange() {
        return new TopicExchange(BLOCK_EXCHANGE);
    }

    @Bean
    public TopicExchange blockRetryExchange() {
        return new TopicExchange(BLOCK_RETRY_EXCHANGE);
    }

    @Bean
    public Queue userRegisteredQueueAdoptionModule() {
        return QueueBuilder
                .durable(USER_REGISTERED_QUEUE_ADOPTION_MODULE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, USER_RETRY_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_REGISTERED_ADOPTION_RETRY)
                .build();
    }

    @Bean
    public Queue userRegisteredQueueAdoptionModuleRetry() {
        return QueueBuilder
                .durable(USER_REGISTERED_QUEUE_ADOPTION_MODULE_RETRY)
                .withArgument(X_MESSAGE_TTL, MESSAGE_TTL_VALUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, USER_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_REGISTERED_ADOPTION)
                .build();
    }

    @Bean
    public Queue userDeletedQueueAdoptionModule() {
        return QueueBuilder
                .durable(USER_DELETED_QUEUE_ADOPTION_MODULE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, USER_RETRY_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_DELETED_ADOPTION_RETRY)
                .build();
    }

    @Bean
    public Queue userDeletedQueueAdoptionModuleRetry() {
        return QueueBuilder
                .durable(USER_DELETED_QUEUE_ADOPTION_MODULE_RETRY)
                .withArgument(X_MESSAGE_TTL, MESSAGE_TTL_VALUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, USER_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_DELETED_ADOPTION)
                .build();
    }

    @Bean
    public Queue userBlockedQueueAdoptionModule() {
        return QueueBuilder
                .durable(USER_BLOCKED_QUEUE_ADOPTION_MODULE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, BLOCK_RETRY_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_BLOCKED_ADOPTION_RETRY)
                .build();
    }

    @Bean
    public Queue userBlockedQueueAdoptionModuleRetry() {
        return QueueBuilder
                .durable(USER_BLOCKED_QUEUE_ADOPTION_MODULE_RETRY)
                .withArgument(X_MESSAGE_TTL, MESSAGE_TTL_VALUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, BLOCK_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_BLOCKED_ADOPTION)
                .build();
    }

    @Bean
    public Queue userUnBlockedQueueAdoptionModule() {
        return QueueBuilder
                .durable(USER_UNBLOCKED_QUEUE_ADOPTION_MODULE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, BLOCK_RETRY_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_UNBLOCKED_ADOPTION_RETRY)
                .build();
    }

    @Bean
    public Queue userUnBlockedQueueAdoptionModuleRetry() {
        return QueueBuilder
                .durable(USER_UNBLOCKED_QUEUE_ADOPTION_MODULE_RETRY)
                .withArgument(X_MESSAGE_TTL, MESSAGE_TTL_VALUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, BLOCK_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, USER_UNBLOCKED_ADOPTION)
                .build();
    }

    // Main Bindings
    @Bean
    public Binding userRegistrationAdoptionModuleBinding(Queue userRegisteredQueueAdoptionModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userRegisteredQueueAdoptionModule).to(userExchange).with(USER_REGISTERED);
    }

    @Bean
    public Binding userDeletionAdoptionModuleBinding(Queue userDeletedQueueAdoptionModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQueueAdoptionModule).to(userExchange).with(USER_DELETED);
    }

    @Bean
    public Binding userBlockingAdoptionBinding(Queue userBlockedQueueAdoptionModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userBlockedQueueAdoptionModule).to(blockExchange).with(BLOCK_ADD);
    }

    @Bean
    public Binding userUnBlockingAdoptionBinding(Queue userUnBlockedQueueAdoptionModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userUnBlockedQueueAdoptionModule).to(blockExchange).with(BLOCK_DELETE);
    }

    // Retry Bindings
    @Bean
    public Binding userRegistrationAdoptionModuleRetryBinding(Queue userRegisteredQueueAdoptionModuleRetry, TopicExchange userRetryExchange) {
        return BindingBuilder.bind(userRegisteredQueueAdoptionModuleRetry).to(userRetryExchange).with(USER_REGISTERED_ADOPTION_RETRY);
    }

    @Bean
    public Binding userDeletionAdoptionModuleRetryBinding(Queue userDeletedQueueAdoptionModuleRetry, TopicExchange userRetryExchange) {
        return BindingBuilder.bind(userDeletedQueueAdoptionModuleRetry).to(userRetryExchange).with(USER_DELETED_ADOPTION_RETRY);
    }

    @Bean
    public Binding userBlockingAdoptionRetryBinding(Queue userBlockedQueueAdoptionModuleRetry, TopicExchange blockRetryExchange) {
        return BindingBuilder.bind(userBlockedQueueAdoptionModuleRetry).to(blockRetryExchange).with(USER_BLOCKED_ADOPTION_RETRY);
    }

    @Bean
    public Binding userUnBlockingAdoptionRetryBinding(Queue userUnBlockedQueueAdoptionModuleRetry, TopicExchange blockRetryExchange) {
        return BindingBuilder.bind(userUnBlockedQueueAdoptionModuleRetry).to(blockRetryExchange).with(USER_UNBLOCKED_ADOPTION_RETRY);
    }

    // Return from Retry Bindings
    @Bean
    public Binding userRegistrationAdoptionModuleRetryReturnBinding(Queue userRegisteredQueueAdoptionModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userRegisteredQueueAdoptionModule).to(userExchange).with(USER_REGISTERED_ADOPTION);
    }

    @Bean
    public Binding userDeletionAdoptionModuleRetryReturnBinding(Queue userDeletedQueueAdoptionModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQueueAdoptionModule).to(userExchange).with(USER_DELETED_ADOPTION);
    }

    @Bean
    public Binding userBlockingAdoptionRetryReturnBinding(Queue userBlockedQueueAdoptionModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userBlockedQueueAdoptionModule).to(blockExchange).with(USER_BLOCKED_ADOPTION);
    }

    @Bean
    public Binding userUnBlockingAdoptionRetryReturnBinding(Queue userUnBlockedQueueAdoptionModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userUnBlockedQueueAdoptionModule).to(blockExchange).with(USER_UNBLOCKED_ADOPTION);
    }
}
