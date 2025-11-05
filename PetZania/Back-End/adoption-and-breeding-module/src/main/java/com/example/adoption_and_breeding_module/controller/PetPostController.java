package com.example.adoption_and_breeding_module.controller;

import com.example.adoption_and_breeding_module.annotation.RateLimit;
import com.example.adoption_and_breeding_module.model.dto.CreatePetPostDTO;
import com.example.adoption_and_breeding_module.model.dto.PetPostDTO;
import com.example.adoption_and_breeding_module.model.dto.PetPostFilterDTO;
import com.example.adoption_and_breeding_module.model.dto.UpdatePetPostDTO;
import com.example.adoption_and_breeding_module.model.enumeration.InterestType;
import com.example.adoption_and_breeding_module.model.principal.UserPrincipal;
import com.example.adoption_and_breeding_module.service.IPetPostService;
import com.example.adoption_and_breeding_module.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/pet-posts")
@RequiredArgsConstructor
@Tag(name = "PetPost", description = "Endpoints for managing pet posts (adoption & breeding)")
public class PetPostController {
    private final IPetPostService petPostService;

    @Operation(summary = "Create a new pet post")
    @PostMapping
    @RateLimit
    public ResponseEntity<PetPostDTO> createPetPost(@Valid @RequestBody CreatePetPostDTO createPetPostDTO) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID ownerId = userPrincipal.getUserId();
        PetPostDTO petPostDTO = petPostService.createPetPost(createPetPostDTO, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(petPostDTO);
    }

    @Operation(summary = "Get all pet posts created by a specific user")
    @GetMapping(path = "/user/{userId}")
    @RateLimit
    public ResponseEntity<Page<PetPostDTO>> getAllPetPostsByUserId(@PathVariable(name = "userId") UUID userId,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID requesterUserId = userPrincipal.getUserId();
        Page<PetPostDTO> posts = petPostService.getAllPetPostsByUserId(requesterUserId, userId, page, size);
        return ResponseEntity.ok(posts);
    }

    @Operation(summary = "Get a pet post by its ID")
    @GetMapping(path = "/{petPostId}")
    @RateLimit
    public ResponseEntity<PetPostDTO> getPetPostById(@PathVariable(name = "petPostId") UUID petPostId) {
        PetPostDTO petPostDTO = petPostService.getPetPostById(petPostId);
        return ResponseEntity.ok(petPostDTO);
    }


    @Operation(summary = "Get filtered pet posts based on search criteria")
    @PostMapping(path = "/filtered")
    @RateLimit
    public ResponseEntity<Page<PetPostDTO>> getFilteredPosts(
            @RequestBody PetPostFilterDTO filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID requesterUserId = userPrincipal.getUserId();
        Page<PetPostDTO> posts = petPostService.getFilteredPosts(requesterUserId, filter, page, size);
        return ResponseEntity.ok(posts);
    }

    @Operation(summary = "Update an existing pet post by ID")
    @PatchMapping(path = "/{petPostId}")
    @RateLimit
    public ResponseEntity<PetPostDTO> updatePetPostById(@PathVariable(name = "petPostId") UUID petPostId,
                                                        @Valid @RequestBody UpdatePetPostDTO updatePetPostDTO) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        PetPostDTO petPostDTO = petPostService.updatePetPost(petPostId, updatePetPostDTO, userId);
        return ResponseEntity.ok(petPostDTO);
    }

    @Operation(summary = "Delete a pet post by ID")
    @DeleteMapping(path = "/{petPostId}")
    @RateLimit
    public ResponseEntity<Void> deletePetPostById(@PathVariable(name = "petPostId") UUID petPostId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        petPostService.deletePetPostById(petPostId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Toggle react status for a pet post by the current user")
    @PutMapping("/{petPostId}/react")
    @RateLimit
    public ResponseEntity<PetPostDTO> toggleReact(@PathVariable(name = "petPostId") UUID petPostId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        PetPostDTO updatedPost = petPostService.toggleReact(petPostId, userId);
        return ResponseEntity.ok(updatedPost);
    }

    @Operation(summary = "Mark a pet post as interested")
    @PutMapping("/{petPostId}/interested")
    public ResponseEntity<Void> markInterested(@PathVariable(name = "petPostId") UUID petPostId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        petPostService.markInterest(petPostId, userId, InterestType.INTERESTED);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Mark a pet post as not interested")
    @PutMapping("/{petPostId}/not-interested")
    public ResponseEntity<Void> markNotInterested(@PathVariable(name = "petPostId") UUID petPostId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        petPostService.markInterest(petPostId, userId, InterestType.NOT_INTERESTED);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove interest mark from a pet post")
    @DeleteMapping("/{petPostId}/interest")
    public ResponseEntity<Void> removeInterest(@PathVariable(name = "petPostId") UUID petPostId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        petPostService.removeInterest(petPostId, userId);
        return ResponseEntity.noContent().build();
    }
}
