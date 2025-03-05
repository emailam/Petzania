package com.example.registrationmodule.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 400 bad request
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ExpiredOTP extends RuntimeException {
    public ExpiredOTP(String message){
        super(message);
    }
}
