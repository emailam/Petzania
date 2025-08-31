package com.example.friends.and.chats.module.constant;

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
    public static final String NOTIFICATION_QUEUE = "notificationsQueue";
    public static final String USER_BLOCKED_QUEUE_REGISTRATION_MODULE = "userBlockedQueueRegistrationModule";
    public static final String USER_BLOCKED_QUEUE_ADOPTION_MODULE = "userBlockedQueueAdoptionModule";
    public static final String USER_DELETED_QUEUE_FRIENDS_MODULE = "userDeletedQueueFriendsModule";
    public static final String USER_REGISTERED_QUEUE_FRIENDS_MODULE = "userRegisteredQueueFriendsModule";
    public static final String USER_UNBLOCKED_QUEUE_REGISTRATION_MODULE = "userUnBlockedQueueRegistrationModule";
    public static final String USER_UNBLOCKED_QUEUE_ADOPTION_MODULE = "userUnBlockedQueueAdoptionModule";

    // ===== Retry Queues' Names =====
    public static final String USER_BLOCKED_QUEUE_ADOPTION_MODULE_RETRY = "userBlockedQueueAdoptionModule.retry";
    public static final String USER_BLOCKED_QUEUE_REGISTRATION_MODULE_RETRY = "userBlockedQueueRegistrationModule.retry";
    public static final String USER_DELETED_QUEUE_FRIENDS_MODULE_RETRY = "userDeletedQueueFriendsModule.retry";
    public static final String USER_REGISTERED_QUEUE_FRIENDS_MODULE_RETRY = "userRegisteredQueueFriendsModule.retry";
    public static final String USER_UNBLOCKED_QUEUE_ADOPTION_MODULE_RETRY = "userUnBlockedQueueAdoptionModule.retry";
    public static final String USER_UNBLOCKED_QUEUE_REGISTRATION_MODULE_RETRY = "userUnBlockedQueueRegistrationModule.retry";

    // RabbitMQ Arguments
    public static final String ACK_MODE = "MANUAL";
    public static final int MESSAGE_TTL_VALUE = 30000;
    public static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    public static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
    public static final String X_MESSAGE_TTL = "x-message-ttl";

    // Main Routing Keys
    public static final String BLOCK_ADD = "block.add";
    public static final String BLOCK_DELETE = "block.delete";
    public static final String NOTIFICATION_ASTERISK = "notification.*";
    public static final String USER_BLOCKED_ADOPTION = "user.blocked.adoption";
    public static final String USER_BLOCKED_REGISTRATION = "user.blocked.registration";
    public static final String USER_DELETED = "user.deleted";
    public static final String USER_DELETED_FRIENDS = "user.deleted.friends";
    public static final String USER_REGISTERED = "user.registered";
    public static final String USER_REGISTERED_FRIENDS = "user.registered.friends";
    public static final String USER_UNBLOCKED_ADOPTION = "user.unblocked.adoption";
    public static final String USER_UNBLOCKED_REGISTRATION = "user.unblocked.registration";
    public static final String NOTIFICATION_FRIEND_REQUEST_RECEIVED = "notification.friend_request_received";
    public static final String NOTIFICATION_FRIEND_REQUEST_ACCEPTED = "notification.friend_request_accepted";
    public static final String NOTIFICATION_NEW_FOLLOWER = "notification.new_follower";
    public static final String NOTIFICATION_FRIEND_REQUEST_CANCELLED = "notification.friend_request_cancelled";
    // Retry Routing Keys
    public static final String USER_BLOCKED_ADOPTION_RETRY = "user.blocked.adoption.retry";
    public static final String USER_BLOCKED_REGISTRATION_RETRY = "user.blocked.registration.retry";
    public static final String USER_DELETED_FRIENDS_RETRY = "user.deleted.friends.retry";
    public static final String USER_REGISTERED_FRIENDS_RETRY = "user.registered.friends.retry";
    public static final String USER_UNBLOCKED_ADOPTION_RETRY = "user.unblocked.adoption.retry";
    public static final String USER_UNBLOCKED_REGISTRATION_RETRY = "user.unblocked.registration.retry";
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