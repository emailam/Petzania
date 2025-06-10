package com.example.friends.and.chats.module.exception.message;

// 400 Bad Request
public class InvalidMessageStatusTransition extends RuntimeException {
    public InvalidMessageStatusTransition(String message) {
        super(message);
    }
}
