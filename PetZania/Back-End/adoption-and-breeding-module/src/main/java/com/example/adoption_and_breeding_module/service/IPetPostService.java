package com.example.adoption_and_breeding_module.service;

import com.example.adoption_and_breeding_module.model.dto.CreatePetPostDTO;
import com.example.adoption_and_breeding_module.model.dto.PetPostDTO;
import com.example.adoption_and_breeding_module.model.dto.PetPostFilterDTO;
import com.example.adoption_and_breeding_module.model.dto.UpdatePetPostDTO;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface IPetPostService {

    PetPostDTO createPetPost(CreatePetPostDTO createPetPostDTO, UUID ownerId);

    PetPostDTO getPetPostById(UUID petPostId);

    PetPostDTO updatePetPost(UUID petPostId, UpdatePetPostDTO updatePetPostDTO, UUID userId);

    void deletePetPostById(UUID petPostId, UUID userId);

    PetPostDTO toggleReact(UUID postId, UUID userId);


    Page<PetPostDTO> getFilteredPosts(UUID userId, PetPostFilterDTO filter, int page, int size);

    Page<PetPostDTO> getAllPetPostsByUserId(UUID requesterUserId, UUID userId, int page, int size);
}
