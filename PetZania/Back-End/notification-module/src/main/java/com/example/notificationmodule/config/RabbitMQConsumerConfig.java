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
