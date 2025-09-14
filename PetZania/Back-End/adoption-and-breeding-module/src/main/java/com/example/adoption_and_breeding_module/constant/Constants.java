package com.example.adoption_and_breeding_module.constant;

public final class Constants {
    private Constants() {
    }

    ; // singleton pattern

    // ===== Exchanges =====
    public static final String BLOCK_EXCHANGE = "blockExchange";
    public static final String BLOCK_RETRY_EXCHANGE = "blockRetryExchange";
    public static final String NOTIFICATION_EXCHANGE = "notificationExchange";
    public static final String USER_EXCHANGE = "userExchange";
    public static final String USER_RETRY_EXCHANGE = "userRetryExchange";
    // ===== Main Queues' Names =====
    public static final String NOTIFICATIONS_QUEUE = "notificationsQueue";
    public static final String USER_BLOCKED_QUEUE_ADOPTION_MODULE = "userBlockedQueueAdoptionModule";
    public static final String USER_DELETED_QUEUE_ADOPTION_MODULE = "userDeletedQueueAdoptionModule";
    public static final String USER_REGISTERED_QUEUE_ADOPTION_MODULE = "userRegisteredQueueAdoptionModule";
    public static final String USER_UNBLOCKED_QUEUE_ADOPTION_MODULE = "userUnBlockedQueueAdoptionModule";

    // ===== Retry Queues' Names =====
    public static final String USER_BLOCKED_QUEUE_ADOPTION_MODULE_RETRY = "userBlockedQueueAdoptionModule.retry";
    public static final String USER_DELETED_QUEUE_ADOPTION_MODULE_RETRY = "userDeletedQueueAdoptionModule.retry";
    public static final String USER_REGISTERED_QUEUE_ADOPTION_MODULE_RETRY = "userRegisteredQueueAdoptionModule.retry";
    public static final String USER_UNBLOCKED_QUEUE_ADOPTION_MODULE_RETRY = "userUnBlockedQueueAdoptionModule.retry";

    // RabbitMQ Arguments
    public static final String ACK_MODE = "MANUAL";
    public static int MESSAGE_TTL_VALUE = 30000;
    public static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    public static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
    public static final String X_MESSAGE_TTL = "x-message-ttl";
    public static final int MAX_RETRIES = 3;

    // Main Routing Keys
    public static final String BLOCK_ADD = "block.add";
    public static final String BLOCK_DELETE = "block.delete";
    public static final String NOTIFICATION_ASTERISK = "notification.*";
    public static final String USER_BLOCKED_ADOPTION = "user.blocked.adoption";
    public static final String USER_DELETED = "user.deleted";
    public static final String USER_DELETED_ADOPTION = "user.deleted.adoption";
    public static final String USER_REGISTERED = "user.registered";
    public static final String USER_REGISTERED_ADOPTION = "user.registered.adoption";
    public static final String USER_UNBLOCKED_ADOPTION = "user.unblocked.adoption";
    public static final String NOTIFICATION_PET_POST_LIKED = "notification.pet_post_liked";
    public static final String NOTIFICATION_PET_POST_DELETED = "notification.pet_post_deleted";
    // Retry Routing Keys
    public static final String USER_BLOCKED_ADOPTION_RETRY = "user.blocked.adoption.retry";
    public static final String USER_DELETED_ADOPTION_RETRY = "user.deleted.adoption.retry";
    public static final String USER_REGISTERED_ADOPTION_RETRY = "user.registered.adoption.retry";
    public static final String USER_UNBLOCKED_ADOPTION_RETRY = "user.unblocked.adoption.retry";
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