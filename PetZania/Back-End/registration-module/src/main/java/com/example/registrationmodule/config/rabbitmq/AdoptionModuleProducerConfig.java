package com.example.registrationmodule.config.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.registrationmodule.constant.Constants.*;


@Configuration
public class AdoptionModuleProducerConfig {
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(USER_EXCHANGE);
    }

    @Bean
    public TopicExchange userRetryExchange() {
        return new TopicExchange(USER_RETRY_EXCHANGE);
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
    public Binding userRegistrationAdoptionModuleBinding(Queue userRegisteredQueueAdoptionModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userRegisteredQueueAdoptionModule).to(userExchange).with(USER_REGISTERED);
    }

    @Bean
    public Binding userDeletionAdoptionModuleBinding(Queue userDeletedQueueAdoptionModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQueueAdoptionModule).to(userExchange).with(USER_DELETED);
    }

    @Bean
    public Binding userRegistrationAdoptionModuleRetryBinding(Queue userRegisteredQueueAdoptionModuleRetry, TopicExchange userRetryExchange) {
        return BindingBuilder.bind(userRegisteredQueueAdoptionModuleRetry).to(userRetryExchange).with(USER_REGISTERED_ADOPTION_RETRY);
    }

    @Bean
    public Binding userDeletionAdoptionModuleRetryBinding(Queue userDeletedQueueAdoptionModuleRetry, TopicExchange userRetryExchange) {
        return BindingBuilder.bind(userDeletedQueueAdoptionModuleRetry).to(userRetryExchange).with(USER_DELETED_ADOPTION_RETRY);
    }

    @Bean
    public Binding userRegistrationAdoptionModuleRetryReturnBinding(Queue userRegisteredQueueAdoptionModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userRegisteredQueueAdoptionModule).to(userExchange).with(USER_REGISTERED_ADOPTION);
    }

    @Bean
    public Binding userDeletionAdoptionModuleRetryReturnBinding(Queue userDeletedQueueAdoptionModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQueueAdoptionModule).to(userExchange).with(USER_DELETED_ADOPTION);
    }

}
