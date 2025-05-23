package com.example.friends.and.chats.module.exception.user;

// 403 forbidden
public class InvalidFriendRequest extends RuntimeException {
    public InvalidFriendRequest(String message) {
        super(message);
    }
}
