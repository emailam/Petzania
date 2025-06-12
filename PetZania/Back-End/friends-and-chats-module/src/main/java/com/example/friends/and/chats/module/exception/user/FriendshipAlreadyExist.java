package com.example.friends.and.chats.module.exception.user;

public class FriendshipAlreadyExist extends RuntimeException {
    public FriendshipAlreadyExist(String message) {
        super(message);
    }
}
