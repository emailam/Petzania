package com.example.registrationmodule.exception;

public class RefreshTokenNotValid extends RuntimeException {
    public RefreshTokenNotValid(String message) {
        super(message);
    }
}
