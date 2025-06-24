package com.example.adoption_and_breeding_module.exception;

// 404 not found
public class PetPostNotFound extends RuntimeException {
    public PetPostNotFound(String message) {
        super(message);
    }
}
