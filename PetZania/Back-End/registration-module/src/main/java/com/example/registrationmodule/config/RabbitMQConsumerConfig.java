package com.example.registrationmodule.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConsumerConfig {

    @Bean
    public TopicExchange blockExchange() {
        return new TopicExchange("blockExchange");
    }

    @Bean
    public Queue userBlockedQueueRegistrationModule() {
        return new Queue("userBlockedQueueRegistrationModule", true);
    }

    @Bean
    public Queue userUnBlockedQueueRegistrationModule() {
        return new Queue("userUnBlockedQueueRegistrationModule", true);
    }

    @Bean
    public Binding userBlockingRegistrationBinding(Queue userBlockedQueueRegistrationModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userBlockedQueueRegistrationModule).to(blockExchange).with("block.add");
    }

    @Bean
    public Binding userUnBlockingRegistrationBinding(Queue userUnBlockedQueueRegistrationModule, TopicExchange blockExchange) {
        return BindingBuilder.bind(userUnBlockedQueueRegistrationModule).to(blockExchange).with("block.delete");
    }

}
