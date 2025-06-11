package com.example.registrationmodule.exception.media;

// 400 bad request
public class InvalidMediaFile extends RuntimeException {
    public InvalidMediaFile(String message) {
        super(message);
    }
}

