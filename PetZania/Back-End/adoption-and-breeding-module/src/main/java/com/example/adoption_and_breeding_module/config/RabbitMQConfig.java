package com.example.adoption_and_breeding_module.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange("userExchange");
    }

    @Bean
    public TopicExchange blockExchange() {
        return new TopicExchange("blockExchange");
    }

    @Bean
    public Queue userRegisteredQueue() {
        return new Queue("userRegisteredQueue", true);
    }

    @Bean
    public Queue userDeletedQueue() {
        return new Queue("userDeletedQueue", true);
    }

    @Bean
    public Queue userBlockedQueue() {
        return new Queue("userBlockedQueue", true);
    }

    @Bean
    public Queue userUnBlockedQueue() {
        return new Queue("userUnBlockedQueue", true);
    }

    @Bean
    public Binding userRegistrationBinding(Queue userRegisteredQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userRegisteredQueue).to(userExchange).with("user.registered");
    }

    @Bean
    public Binding userDeletionBinding(Queue userDeletedQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQueue).to(userExchange).with("user.deleted");
    }

    @Bean
    public Binding userBlockingBinding(Queue userBlockedQueue, TopicExchange blockExchange) {
        return BindingBuilder.bind(userBlockedQueue).to(blockExchange).with("block.add");
    }

    @Bean
    public Binding userUnBlockingBinding(Queue userUnBlockedQueue, TopicExchange blockExchange) {
        return BindingBuilder.bind(userUnBlockedQueue).to(blockExchange).with("block.delete");
    }
}
