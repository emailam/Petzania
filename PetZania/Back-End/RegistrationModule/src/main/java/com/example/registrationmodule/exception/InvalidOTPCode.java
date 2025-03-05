package com.example.registrationmodule.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 400 bad request
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidOTPCode extends RuntimeException {
    public InvalidOTPCode(String message) {
        super(message);
    }
}
