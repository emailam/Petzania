package com.example.friends.and.chats.module.exception.admin;

// 404 not found
public class AdminNotFound extends RuntimeException {
    public AdminNotFound(String message) {
        super(message);
    }
}
