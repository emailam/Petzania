package com.example.registrationmodule.exception.authenticationAndVerificattion;

public class InvalidToken extends RuntimeException {
    public InvalidToken(String message){
        super(message);
    }
}
