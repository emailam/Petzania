package com.example.friends.and.chats.module.exception.user;

public class FriendshipDoesNotExist extends RuntimeException{
    public FriendshipDoesNotExist(String message){
        super(message);
    }
}
