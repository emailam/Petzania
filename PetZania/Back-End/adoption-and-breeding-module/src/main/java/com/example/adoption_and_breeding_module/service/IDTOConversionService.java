package com.example.adoption_and_breeding_module.service;

import com.example.adoption_and_breeding_module.model.dto.PetDTO;
import com.example.adoption_and_breeding_module.model.dto.PetPostDTO;
import com.example.adoption_and_breeding_module.model.entity.Pet;
import com.example.adoption_and_breeding_module.model.entity.PetPost;

public interface IDTOConversionService {
    Pet mapToPet(PetDTO petDTO);

    PetPostDTO mapToPetPostDTO(PetPost post);
}
