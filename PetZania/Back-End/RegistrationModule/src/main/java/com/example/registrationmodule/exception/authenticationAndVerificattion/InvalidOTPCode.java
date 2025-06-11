package com.example.registrationmodule.exception.authenticationAndVerificattion;

// 400 bad request
public class InvalidOTPCode extends RuntimeException {
    public InvalidOTPCode(String message) {
        super(message);
    }
}
