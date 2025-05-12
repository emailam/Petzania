package com.example.friends.and.chats.module.exception.message;

// 409 Conflict
public class MessageNotUpdatable extends RuntimeException {
    public MessageNotUpdatable(String message) {
        super(message);
    }
}
