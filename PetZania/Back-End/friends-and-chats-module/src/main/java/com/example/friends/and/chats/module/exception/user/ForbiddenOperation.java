package com.example.friends.and.chats.module.exception.user;

public class ForbiddenOperation extends RuntimeException {
    public ForbiddenOperation(String message) {
        super(message);
    }
}
