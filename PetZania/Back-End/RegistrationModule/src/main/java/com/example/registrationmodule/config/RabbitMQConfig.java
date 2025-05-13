package com.example.registrationmodule.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    // Queue names
    public static final String USER_CREATED_QUEUE = "user-created-queue";
    public static final String USER_CREATED_DLQ = "user-created-dlq";

    // Exchange names
    public static final String USER_EXCHANGE = "user-exchange";
    public static final String DEAD_LETTER_EXCHANGE = "dead-letter-exchange";

    // Routing keys
    public static final String USER_CREATED_ROUTING_KEY = "user.created";
    public static final String DEAD_LETTER_ROUTING_KEY = "dead.letter";

    // Retry configuration
    @Value("${rabbitmq.retry.initial-interval:1000}")
    private long initialInterval;

    @Value("${rabbitmq.retry.max-interval:10000}")
    private long maxInterval;

    @Value("${rabbitmq.retry.multiplier:2.0}")
    private double multiplier;

    @Value("${rabbitmq.retry.max-attempts:3}")
    private int maxAttempts;

    /**
     * Configure JSON message converter for RabbitMQ
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configure RabbitTemplate with message converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    /**
     * Dead Letter Exchange declaration
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    /**
     * Dead Letter Queue declaration
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(USER_CREATED_DLQ)
                .build();
    }

    /**
     * Binding between Dead Letter Exchange and Dead Letter Queue
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DEAD_LETTER_ROUTING_KEY);
    }

    /**
     * User Created Exchange declaration
     */
    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(USER_EXCHANGE);
    }

    /**
     * User Created Queue with Dead Letter configuration
     */
    @Bean
    public Queue userCreatedQueue() {
        Map<String, Object> args = new HashMap<>();
        // Configure the DLX and DLK for this queue
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY);

        return QueueBuilder.durable(USER_CREATED_QUEUE)
                .withArguments(args)
                .build();
    }

    /**
     * Binding between User Exchange and User Created Queue
     */
    @Bean
    public Binding userCreatedBinding() {
        return BindingBuilder.bind(userCreatedQueue())
                .to(userExchange())
                .with(USER_CREATED_ROUTING_KEY);
    }

    /**
     * Retry policy for message handling
     */
    @Bean
    public RetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(maxAttempts)
                .backOffOptions(initialInterval, multiplier, maxInterval)
                .build();
    }

    /**
     * Configure listener container factory with retry and error handling
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());

        // Configure prefetch count (concurrent messages per consumer)
        factory.setPrefetchCount(1);

        // Set default requeue to false (messages won't return to queue on failure)
        factory.setDefaultRequeueRejected(false);

        // Add retry interceptor for automatic retries
        factory.setAdviceChain(retryInterceptor());

        return factory;
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}

