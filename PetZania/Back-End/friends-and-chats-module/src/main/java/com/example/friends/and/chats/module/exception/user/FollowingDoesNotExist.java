package com.example.friends.and.chats.module.exception.user;

public class FollowingDoesNotExist extends RuntimeException{
    public FollowingDoesNotExist(String message){
        super(message);
    }
}
