package com.example.friends.and.chats.module.exception.chat;

// 404 not found
public class UserChatNotFound extends RuntimeException {
    public UserChatNotFound(String message) {
        super(message);
    }
}
