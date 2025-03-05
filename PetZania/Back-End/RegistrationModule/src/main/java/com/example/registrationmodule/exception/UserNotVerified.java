package com.example.registrationmodule.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UserNotVerified extends RuntimeException {
    public UserNotVerified(String message) {
        super(message);
    }
}
