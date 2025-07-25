package com.example.adoption_and_breeding_module.exception;

// 404 not found
public class PetPostInterestNotFound extends RuntimeException {
    public PetPostInterestNotFound(String message) {
        super(message);
    }
}
