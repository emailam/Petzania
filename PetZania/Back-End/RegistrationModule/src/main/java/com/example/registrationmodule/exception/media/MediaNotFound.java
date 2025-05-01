package com.example.registrationmodule.exception.media;

// 404 not found
public class MediaNotFound extends RuntimeException {
    public MediaNotFound(String message) {
        super(message);
    }
}

