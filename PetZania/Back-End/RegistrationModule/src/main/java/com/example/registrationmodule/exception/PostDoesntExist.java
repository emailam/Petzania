package com.example.registrationmodule.exception;

public class PostDoesntExist extends RuntimeException {
    public PostDoesntExist(String message){
        super(message);
    }
}
