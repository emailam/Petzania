package com.example.adoption_and_breeding_module.controller;

import com.example.adoption_and_breeding_module.model.dto.CreatePetPostDTO;
import com.example.adoption_and_breeding_module.model.dto.PetPostDTO;
import com.example.adoption_and_breeding_module.service.IPetPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/pet-posts")
@RequiredArgsConstructor
public class PetPostController {
    private final IPetPostService petPostService;

    @PostMapping()
    public ResponseEntity<PetPostDTO> createPetPost(@RequestBody CreatePetPostDTO createPetPostDTO) {
        // get user1Id from authentication context
        UUID ownerId = UUID.randomUUID();
        PetPostDTO petPostDTO = petPostService.createPetPost(createPetPostDTO, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(petPostDTO);
    }
}
