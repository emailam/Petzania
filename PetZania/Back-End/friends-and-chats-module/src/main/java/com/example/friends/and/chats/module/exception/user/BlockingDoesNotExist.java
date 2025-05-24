package com.example.friends.and.chats.module.exception.user;

public class BlockingDoesNotExist extends RuntimeException {
    public BlockingDoesNotExist(String message){
        super(message);
    }
}
