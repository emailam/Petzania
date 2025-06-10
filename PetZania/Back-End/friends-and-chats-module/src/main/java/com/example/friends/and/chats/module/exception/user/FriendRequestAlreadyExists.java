package com.example.friends.and.chats.module.exception.user;

public class FriendRequestAlreadyExists extends RuntimeException {
    public FriendRequestAlreadyExists(String message) {
        super(message);
    }
}
