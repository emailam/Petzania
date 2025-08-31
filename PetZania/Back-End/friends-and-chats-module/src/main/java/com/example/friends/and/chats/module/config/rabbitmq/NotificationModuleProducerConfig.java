package com.example.friends.and.chats.module.config.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.friends.and.chats.module.constant.Constants.*;

@Configuration
public class NotificationModuleProducerConfig {
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue notificationsQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Binding notificationsBinding(Queue notificationsQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationsQueue).to(notificationExchange).with(NOTIFICATION_ASTERISK);
    }
}
