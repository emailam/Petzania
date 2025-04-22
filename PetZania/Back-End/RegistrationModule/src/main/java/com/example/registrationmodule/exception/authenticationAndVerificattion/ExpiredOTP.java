package com.example.registrationmodule.exception.authenticationAndVerificattion;

// 400 bad request
public class ExpiredOTP extends RuntimeException {
    public ExpiredOTP(String message){
        super(message);
    }
}
