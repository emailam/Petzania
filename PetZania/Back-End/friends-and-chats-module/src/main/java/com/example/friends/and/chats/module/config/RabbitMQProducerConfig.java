package com.example.friends.and.chats.module.config;

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
    public TopicExchange blockExchange() {
        return new TopicExchange("blockExchange");
    }

    @Bean
    public TopicExchange friendExchange() {
        return new TopicExchange("friendExchange");
    }

    @Bean
    public TopicExchange followExchange() {
        return new TopicExchange("followExchange");
    }

    @Bean
    public Queue userBlockedQueueRegistrationModule() {
        return new Queue("userBlockedQueueRegistrationModule", true);
    }

    @Bean
    public Queue userBlockedQueueAdoptionModule() {
        return new Queue("userBlockedQueueAdoptionModule", true);
    }

    @Bean
    public Queue userUnBlockedQueueRegistrationModule() {
        return new Queue("userUnBlockedQueueRegistrationModule", true);
    }

    @Bean
    public Queue userUnBlockedQueueAdoptionModule() {
        return new Queue("userUnBlockedQueueAdoptionModule", true);
    }

    @Bean
    public Queue friendAddedQueueAdoptionModule() {
        return new Queue("friendAddedQueueAdoptionModule", true);
    }

    @Bean
    public Queue friendRemovedQueueAdoptionModule() {
        return new Queue("friendRemovedQueueAdoptionModule", true);
    }

    @Bean
    public Queue followAddedQueueAdoptionModule() {
        return new Queue("followAddedQueueAdoptionModule", true);
    }

    @Bean
    public Queue followRemovedQueueAdoptionModule() {
        return new Queue("followRemovedQueueAdoptionModule", true);
    }

    @Bean
    public Binding userBlockingRegistrationBinding(Queue userBlockedQueueRegistrationModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userBlockedQueueRegistrationModule).to(blockExchange).with("block.add");
    }

    @Bean
    public Binding userBlockingAdoptionBinding(Queue userBlockedQueueAdoptionModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userBlockedQueueAdoptionModule).to(blockExchange).with("block.add");
    }

    @Bean
    public Binding userUnBlockingRegistrationBinding(Queue userUnBlockedQueueRegistrationModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userUnBlockedQueueRegistrationModule).to(blockExchange).with("block.delete");
    }

    @Bean
    public Binding userUnBlockingAdoptionBinding(Queue userUnBlockedQueueAdoptionModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userUnBlockedQueueAdoptionModule).to(blockExchange).with("block.delete");
    }

    @Bean
    public Binding friendAddedBinding(Queue friendAddedQueueAdoptionModule, TopicExchange friendExchange) {
        return BindingBuilder.bind(friendAddedQueueAdoptionModule).to(friendExchange).with("friend.added");
    }

    @Bean
    public Binding friendRemovedBinding(Queue friendRemovedQueueAdoptionModule, TopicExchange friendExchange) {
        return BindingBuilder.bind(friendRemovedQueueAdoptionModule).to(friendExchange).with("friend.removed");
    }

    @Bean
    public Binding followAddedBinding(Queue followAddedQueueAdoptionModule, TopicExchange followExchange) {
        return BindingBuilder.bind(followAddedQueueAdoptionModule).to(followExchange).with("follow.added");
    }

    @Bean
    public Binding followRemovedBinding(Queue followRemovedQueueAdoptionModule, TopicExchange followExchange) {
        return BindingBuilder.bind(followRemovedQueueAdoptionModule).to(followExchange).with("follow.removed");
    }

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange("notificationExchange");
    }

    @Bean
    public Queue notificationsQueue() {
        return new Queue("notificationsQueue", true);
    }

    @Bean
    public Binding notificationsBinding(Queue notificationsQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationsQueue).to(notificationExchange).with("notification.*");
    }

}