package com.example.registrationmodule.exception;

// 500 internal server error
public class EmailNotSent extends RuntimeException {
    public EmailNotSent(String message) {
        super(message);
    }
}