package com.example.registrationmodule.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 500 internal server error
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class EmailNotSent extends RuntimeException {
    public EmailNotSent(String message) {
        super(message);
    }
}