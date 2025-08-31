package com.example.friends.and.chats.module.config.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.friends.and.chats.module.constant.Constants.*;

@Configuration
public class AdoptionModuleProducerConfig {
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
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

    @Bean
    public Binding userBlockingAdoptionBinding(Queue userBlockedQueueAdoptionModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userBlockedQueueAdoptionModule).to(blockExchange).with(BLOCK_ADD);
    }

    @Bean
    public Binding userUnBlockingAdoptionBinding(Queue userUnBlockedQueueAdoptionModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userUnBlockedQueueAdoptionModule).to(blockExchange).with(BLOCK_DELETE);
    }

    @Bean
    public Binding userBlockingAdoptionRetryBinding(Queue userBlockedQueueAdoptionModuleRetry, TopicExchange blockRetryExchange) {
        return BindingBuilder.bind(userBlockedQueueAdoptionModuleRetry).to(blockRetryExchange).with(USER_BLOCKED_ADOPTION_RETRY);
    }

    @Bean
    public Binding userUnBlockingAdoptionRetryBinding(Queue userUnBlockedQueueAdoptionModuleRetry, TopicExchange blockRetryExchange) {
        return BindingBuilder.bind(userUnBlockedQueueAdoptionModuleRetry).to(blockRetryExchange).with(USER_UNBLOCKED_ADOPTION_RETRY);
    }

    // Return from retry bindings
    @Bean
    public Binding userBlockingAdoptionRetryReturnBinding(Queue userBlockedQueueAdoptionModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userBlockedQueueAdoptionModule).to(blockExchange).with(USER_BLOCKED_ADOPTION);
    }

    @Bean
    public Binding userUnBlockingAdoptionRetryReturnBinding(Queue userUnBlockedQueueAdoptionModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userUnBlockedQueueAdoptionModule).to(blockExchange).with(USER_UNBLOCKED_ADOPTION);
    }
}
