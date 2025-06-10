package com.example.friends.and.chats.module.exception.user;

// 403 forbidden

public class UserAccessDenied extends RuntimeException {
    public UserAccessDenied(String message) {
        super(message);
    }
}