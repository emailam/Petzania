package com.example.notificationmodule.constant;

public final class Constants {
    private Constants() {
    }

    ; // singleton pattern

    // ===== Exchanges =====
    public static final String NOTIFICATION_EXCHANGE = "notificationExchange";
    public static final String USER_EXCHANGE = "userExchange";
    public static final String USER_RETRY_EXCHANGE = "userRetryExchange";
    // ===== Main Queues' Names =====
    public static final String NOTIFICATIONS_QUEUE = "notificationsQueue";
    public static final String USER_DELETED_QUEUE_NOTIFICATION_MODULE = "userDeletedQueueNotificationModule";
    public static final String USER_REGISTERED_QUEUE_NOTIFICATION_MODULE = "userRegisteredQueueNotificationModule";
    // ===== Retry Queues' Names =====
    public static final String USER_DELETED_QUEUE_NOTIFICATION_MODULE_RETRY = "userDeletedQueueNotificationModule.retry";
    public static final String USER_REGISTERED_QUEUE_NOTIFICATION_MODULE_RETRY = "userRegisteredQueueNotificationModule.retry";

    // RabbitMQ Arguments
    public static final String ACK_MODE = "MANUAL";
    public static final int MESSAGE_TTL_VALUE = 30000;
    public static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    public static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
    public static final String X_MESSAGE_TTL = "x-message-ttl";
    public static final int MAX_RETRIES = 3;

    // Main Routing Keys
    public static final String NOTIFICATION_ASTERISK = "notification.*";
    public static final String USER_DELETED = "user.deleted";
    public static final String USER_DELETED_NOTIFICATION = "user.deleted.notification";
    public static final String USER_REGISTERED = "user.registered";
    public static final String USER_REGISTERED_NOTIFICATION = "user.registered.notification";
    // Retry Routing Keys
    public static final String USER_DELETED_NOTIFICATION_RETRY = "user.deleted.notification.retry";
    public static final String USER_REGISTERED_NOTIFICATION_RETRY = "user.registered.notification.retry";
    // Rate Limit
    public static final int RATE_LIMIT_DEFAULT_REQUESTS = 10;
    public static final int RATE_LIMIT_DEFAULT_DURATION = 60;
    // JWT
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String STARTING_WITH_STRING = "Bearer ";
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    public static final String ANONYMOUS = "anonymous";
    public static final int START_INDEX = 7;

}
