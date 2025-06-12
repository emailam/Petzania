package com.example.friends.and.chats.module.exception.chat;

// 404 not found
public class ChatNotFound extends RuntimeException {
    public ChatNotFound(String message) {
        super(message);
    }
}
