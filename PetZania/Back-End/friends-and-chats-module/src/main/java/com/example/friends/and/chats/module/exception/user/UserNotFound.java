package com.example.friendsAndChatsModule.exception.user;

// 404 not found
public class UserNotFound extends RuntimeException {
    public UserNotFound(String message) {
        super(message);
    }
}
