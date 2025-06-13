package com.example.adoption_and_breeding_module.service;

import com.example.adoption_and_breeding_module.model.dto.CreatePetPostDTO;
import com.example.adoption_and_breeding_module.model.dto.PetPostDTO;

import java.util.UUID;

public interface IPetPostService {

    PetPostDTO createPetPost(CreatePetPostDTO createPetPostDTO, UUID ownerId);
}
