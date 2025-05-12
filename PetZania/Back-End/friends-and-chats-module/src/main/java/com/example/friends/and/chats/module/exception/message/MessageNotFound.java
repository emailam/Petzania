package com.example.friends.and.chats.module.exception.message;

// 404 not found
public class MessageNotFound extends RuntimeException {
    public MessageNotFound(String message) {
        super(message);
    }
}
