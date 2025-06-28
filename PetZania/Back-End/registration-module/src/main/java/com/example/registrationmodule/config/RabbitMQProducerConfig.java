package com.example.registrationmodule.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQProducerConfig {

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange("userExchange");
    }

    @Bean
    public Queue userRegisteredQueueFriendsModule() {
        return new Queue("userRegisteredQueueFriendsModule", true);
    }

    @Bean
    public Queue userRegisteredQueueAdoptionModule() {
        return new Queue("userRegisteredQueueAdoptionModule", true);
    }

    @Bean
    public Queue userRegisteredQueueNotificationModule() {
        return new Queue("userRegisteredQueueNotificationModule", true);
    }

    @Bean
    public Queue userDeletedQueueFriendsModule() {
        return new Queue("userDeletedQueueFriendsModule", true);
    }

    @Bean
    public Queue userDeletedQueueAdoptionModule() {
        return new Queue("userDeletedQueueAdoptionModule", true);
    }

    @Bean
    public Queue userDeletedQueueNotificationModule() {
        return new Queue("userDeletedQueueNotificationModule", true);
    }

    @Bean
    public Binding userRegistrationFriendsModuleBinding(Queue userRegisteredQueueFriendsModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userRegisteredQueueFriendsModule).to(userExchange).with("user.registered");
    }

    @Bean
    public Binding userRegistrationAdoptionModuleBinding(Queue userRegisteredQueueAdoptionModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userRegisteredQueueAdoptionModule).to(userExchange).with("user.registered");
    }

    @Bean
    public Binding userRegistrationNotificationModuleBinding(Queue userRegisteredQueueNotificationModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userRegisteredQueueNotificationModule).to(userExchange).with("user.registered");
    }

    @Bean
    public Binding userDeletionFriendsModuleBinding(Queue userDeletedQueueFriendsModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQueueFriendsModule).to(userExchange).with("user.deleted");
    }

    @Bean
    public Binding userDeletionAdoptionModuleBinding(Queue userDeletedQueueAdoptionModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQueueAdoptionModule).to(userExchange).with("user.deleted");
    }

    @Bean
    public Binding userDeletionNotificationModuleBinding(Queue userDeletedQueueNotificationModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQueueNotificationModule).to(userExchange).with("user.deleted");
    }
}