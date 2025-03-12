package com.example.registrationmodule.exception;

public class InvalidToken extends RuntimeException {
    public InvalidToken(String message){
        super(message);
    }
}
