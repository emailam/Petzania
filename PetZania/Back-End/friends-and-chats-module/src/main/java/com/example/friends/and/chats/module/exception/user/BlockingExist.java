package com.example.friends.and.chats.module.exception.user;

// 403
public class BlockingExist extends RuntimeException {
    public BlockingExist(String message) {
        super(message);
    }
}
