package com.example.adoption_and_breeding_module.repository.projection;

public interface PostTypeScore {
    com.example.adoption_and_breeding_module.model.enumeration.PetPostType getPostType();
    Long getScore();
}