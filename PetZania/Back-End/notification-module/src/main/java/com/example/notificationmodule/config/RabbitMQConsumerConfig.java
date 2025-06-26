package com.example.notificationmodule.config;

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
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Single exchange for all notification events
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange("notificationExchange");
    }

    // Single queue that receives from all modules
    @Bean
    public Queue notificationsQueue() {
        return new Queue("notificationsQueue", true);
    }

    // Single binding that catches all notification events
    @Bean
    public Binding notificationsBinding(Queue notificationsQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationsQueue).to(notificationExchange).with("notification.*");
    }
}
