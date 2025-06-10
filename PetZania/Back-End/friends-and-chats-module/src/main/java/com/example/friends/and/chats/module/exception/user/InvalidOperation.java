package com.example.friends.and.chats.module.exception.user;

// 403 forbidden
public class InvalidOperation extends RuntimeException {
    public InvalidOperation(String message) {
        super(message);
    }
}
