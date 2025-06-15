package com.example.adoption_and_breeding_module.controller;

import com.example.adoption_and_breeding_module.model.dto.CreatePetPostDTO;
import com.example.adoption_and_breeding_module.model.dto.PetPostDTO;
import com.example.adoption_and_breeding_module.model.dto.PetPostFilterDTO;
import com.example.adoption_and_breeding_module.model.dto.UpdatePetPostDTO;
import com.example.adoption_and_breeding_module.service.IPetPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/pet-posts")
@RequiredArgsConstructor
public class PetPostController {
    private final IPetPostService petPostService;

    @PostMapping
    public ResponseEntity<PetPostDTO> createPetPost(@RequestBody CreatePetPostDTO createPetPostDTO) {
        // get userId from authentication context
        UUID ownerId = UUID.randomUUID();
        PetPostDTO petPostDTO = petPostService.createPetPost(createPetPostDTO, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(petPostDTO);
    }

    @GetMapping(path = "/user/{userId}")
    public ResponseEntity<Page<PetPostDTO>> getAllPetPostsByUserId(@PathVariable(name = "userId") UUID userId,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        // get userId from authentication context
        UUID requesterUserId = UUID.randomUUID();
        Page<PetPostDTO> posts = petPostService.getAllPetPostsByUserId(requesterUserId, userId, page, size);
        return ResponseEntity.ok(posts);
    }

    @GetMapping(path = "/{petPostId}")
    public ResponseEntity<PetPostDTO> getPetPostById(@PathVariable(name = "petPostId") UUID petPostId) {
        PetPostDTO petPostDTO = petPostService.getPetPostById(petPostId);
        return ResponseEntity.ok(petPostDTO);
    }

    @GetMapping(path = "/filtered")
    public ResponseEntity<Page<PetPostDTO>> getFilteredPosts(
            @RequestBody PetPostFilterDTO filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // get userId from authentication context
        UUID requesterUserId = UUID.randomUUID();
        Page<PetPostDTO> posts = petPostService.getFilteredPosts(requesterUserId, filter, page, size);
        return ResponseEntity.ok(posts);
    }

    @PatchMapping(path = "/{petPostId}")
    public ResponseEntity<PetPostDTO> updatePetPostById(@PathVariable(name = "petPostId") UUID petPostId,
                                                        @RequestBody UpdatePetPostDTO updatePetPostDTO) {
        // get user1Id from authentication context
        UUID userId = UUID.randomUUID();
        PetPostDTO petPostDTO = petPostService.updatePetPost(petPostId, updatePetPostDTO, userId);
        return ResponseEntity.ok(petPostDTO);
    }

    @DeleteMapping(path = "/{petPostId}")
    public ResponseEntity<Void> deletePetPostById(@PathVariable(name = "petPostId") UUID petPostId) {
        // get user1Id from authentication context
        UUID userId = UUID.randomUUID();
        petPostService.deletePetPostById(petPostId, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{petPostId}/react")
    public ResponseEntity<PetPostDTO> toggleReact(@PathVariable(name = "petPostId") UUID petPostId) {
        // get user1Id from authentication context
        UUID userId = UUID.randomUUID();
        PetPostDTO updatedPost = petPostService.toggleReact(petPostId, userId);
        return ResponseEntity.ok(updatedPost);
    }
}
