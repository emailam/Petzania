package com.example.friends.and.chats.module.exception.user;

// 404 not found
public class UserNotFound extends RuntimeException {
    public UserNotFound(String message) {
        super(message);
    }
}
