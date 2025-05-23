package com.example.friends.and.chats.module.exception.user;

public class FollowingAlreadyExists extends RuntimeException {
    public FollowingAlreadyExists(String message) {
        super(message);
    }
}
