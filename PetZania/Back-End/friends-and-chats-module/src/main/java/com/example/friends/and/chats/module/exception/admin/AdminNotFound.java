package com.example.friendsAndChatsModule.exception.globalhandler.admin;

// 404 not found
public class AdminNotFound extends RuntimeException {
    public AdminNotFound(String message) {
        super(message);
    }
}
