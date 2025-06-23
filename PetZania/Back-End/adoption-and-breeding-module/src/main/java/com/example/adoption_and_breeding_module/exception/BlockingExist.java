package com.example.adoption_and_breeding_module.exception;

// 403
public class BlockingExist extends RuntimeException {
    public BlockingExist(String message) {
        super(message);
    }
}