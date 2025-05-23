package com.example.friends.and.chats.module.exception.user;

public class FriendRequestNotFound extends RuntimeException{
    public FriendRequestNotFound(String message){
        super(message);
    }
}
