package com.example.adoption_and_breeding_module.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConsumerConfig {

    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange("userExchange");
    }

    @Bean
    public TopicExchange blockExchange() {
        return new TopicExchange("blockExchange");
    }

    @Bean
    public Queue userRegisteredQueueAdoptionModule() {
        return new Queue("userRegisteredQueueAdoptionModule", true);
    }

    @Bean
    public Queue userDeletedQueueAdoptionModule() {
        return new Queue("userDeletedQueueAdoptionModule", true);
    }

    @Bean
    public Queue userBlockedQueueAdoptionModule() {
        return new Queue("userBlockedQueueAdoptionModule", true);
    }

    @Bean
    public Queue userUnBlockedQueueAdoptionModule() {
        return new Queue("userUnBlockedQueueAdoptionModule", true);
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
    public Binding userRegistrationAdoptionModuleBinding(Queue userRegisteredQueueAdoptionModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userRegisteredQueueAdoptionModule).to(userExchange).with("user.registered");
    }

    @Bean
    public Binding userDeletionAdoptionModuleBinding(Queue userDeletedQueueAdoptionModule, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQueueAdoptionModule).to(userExchange).with("user.deleted");
    }

    @Bean
    public Binding userBlockingAdoptionBinding(Queue userBlockedQueueAdoptionModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userBlockedQueueAdoptionModule).to(blockExchange).with("block.add");
    }

    @Bean
    public Binding userUnBlockingAdoptionBinding(Queue userUnBlockedQueueAdoptionModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userUnBlockedQueueAdoptionModule).to(blockExchange).with("block.delete");
    }

    @Bean
    public Binding friendAddedAdoptionModuleBinding(Queue friendAddedQueueAdoptionModule, TopicExchange friendExchange) {
        return BindingBuilder.bind(friendAddedQueueAdoptionModule).to(friendExchange).with("friend.added");
    }

    @Bean
    public Binding friendRemovedAdoptionModuleBinding(Queue friendRemovedQueueAdoptionModule, TopicExchange friendExchange) {
        return BindingBuilder.bind(friendRemovedQueueAdoptionModule).to(friendExchange).with("friend.removed");
    }

    @Bean
    public Binding followAddedAdoptionModuleBinding(Queue followAddedQueueAdoptionModule, TopicExchange followExchange) {
        return BindingBuilder.bind(followAddedQueueAdoptionModule).to(followExchange).with("follow.added");
    }

    @Bean
    public Binding followRemovedAdoptionModuleBinding(Queue followRemovedQueueAdoptionModule, TopicExchange followExchange) {
        return BindingBuilder.bind(followRemovedQueueAdoptionModule).to(followExchange).with("follow.removed");
    }
}
