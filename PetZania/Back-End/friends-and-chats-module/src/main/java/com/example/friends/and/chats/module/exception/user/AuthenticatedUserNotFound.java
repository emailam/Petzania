package com.example.friends.and.chats.module.exception.user;

// 404
public class AuthenticatedUserNotFound extends RuntimeException {
    public AuthenticatedUserNotFound(String message) {
        super(message);
    }
}
