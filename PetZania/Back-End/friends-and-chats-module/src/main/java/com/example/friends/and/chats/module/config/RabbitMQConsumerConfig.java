package com.example.friends.and.chats.module.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConsumerConfig {
    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange("userExchange");
    }

    @Bean
    public Queue userRegisteredQueueFriendsModule() {
        return new Queue("userRegisteredQueueFriendsModule", true);
    }

    @Bean
    public Queue userDeletedQueueFriendsModule() {
        return new Queue("userDeletedQueueFriendsModule", true);
    }

    @Bean
    public Binding userRegistrationFriendsModuleBinding(Queue userRegisteredQueueFriendsModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userRegisteredQueueFriendsModule).to(userExchange).with("user.registered");
    }

    @Bean
    public Binding userDeletionFriendsModuleBinding(Queue userDeletedQueueFriendsModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQueueFriendsModule).to(userExchange).with("user.deleted");
    }
}
