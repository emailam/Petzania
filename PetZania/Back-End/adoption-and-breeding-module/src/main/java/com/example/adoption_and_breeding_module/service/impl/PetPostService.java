package com.example.adoption_and_breeding_module.service.impl;

import com.example.adoption_and_breeding_module.exception.PetPostNotFound;
import com.example.adoption_and_breeding_module.exception.UserAccessDenied;
import com.example.adoption_and_breeding_module.exception.UserNotFound;
import com.example.adoption_and_breeding_module.model.dto.*;
import com.example.adoption_and_breeding_module.model.entity.Pet;
import com.example.adoption_and_breeding_module.model.entity.PetPost;
import com.example.adoption_and_breeding_module.model.entity.User;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostType;
import com.example.adoption_and_breeding_module.repository.PetPostRepository;
import com.example.adoption_and_breeding_module.repository.UserRepository;
import com.example.adoption_and_breeding_module.service.IDTOConversionService;
import com.example.adoption_and_breeding_module.service.IPetPostService;
import com.example.adoption_and_breeding_module.util.PetPostSpecification;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class PetPostService implements IPetPostService {

    private final UserRepository userRepository;
    private final PetPostRepository petPostRepository;
    private final IDTOConversionService dtoConversionService;

    @Override
    public PetPostDTO createPetPost(CreatePetPostDTO dto, UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFound("User not found with id: " + ownerId));
        // dummy
//        User owner = User.builder()
//                .userId(UUID.randomUUID())
//                .username("dummy_user")
//                .email("dummy@example.com")
//                .profilePictureURL("https://example.com/default-avatar.png")
//                .build();
//        owner = userRepository.save(owner);

        PetPost post = PetPost.builder()
                .pet(dtoConversionService.mapToPet(dto.getPetDTO()))
                .owner(owner)
                .description(dto.getDescription())
                .postType(dto.getPostType())
                .build();

        post = petPostRepository.save(post);
        return dtoConversionService.mapToPetPostDTO(post);
    }

    @Override
    public Page<PetPostDTO> getAllAdoptionPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PetPost> posts = petPostRepository
                .findAllByPostType(PetPostType.ADOPTION, pageable);
        return posts.map(dtoConversionService::mapToPetPostDTO);
    }

    @Override
    public Page<PetPostDTO> getAllBreedingPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PetPost> posts = petPostRepository
                .findAllByPostType(PetPostType.BREEDING, pageable);
        return posts.map(dtoConversionService::mapToPetPostDTO);
    }

    @Override
    public PetPostDTO getPetPostById(UUID petPostId) {
        PetPost petPost = petPostRepository.findById(petPostId)
                .orElseThrow(() -> new PetPostNotFound("Pet post not found with id: " + petPostId));
        return dtoConversionService.mapToPetPostDTO(petPost);
    }

    @Override
    public PetPostDTO updatePetPost(UUID petPostId,
                                    UpdatePetPostDTO dto,
                                    UUID userId) {
        PetPost post = petPostRepository.findById(petPostId)
                .orElseThrow(() -> new PetPostNotFound("Pet post not found: " + petPostId));

        if (!post.getOwner().getUserId().equals(userId)) {
            throw new UserAccessDenied("You can only update your own posts");
        }

        if (dto.getDescription() != null) {
            post.setDescription(dto.getDescription());
        }
        if (dto.getPostStatus() != null) {
            post.setPostStatus(dto.getPostStatus());
        }

        UpdatePetDTO updatePetDTO = dto.getUpdatePetDTO();
        if (updatePetDTO != null) {
            Pet pet = post.getPet();
            if (updatePetDTO.getName() != null)           pet.setName(updatePetDTO.getName());
            if (updatePetDTO.getDescription() != null)    pet.setDescription(updatePetDTO.getDescription());
            if (updatePetDTO.getGender() != null)         pet.setGender(updatePetDTO.getGender());
            if (updatePetDTO.getBreed() != null)          pet.setBreed(updatePetDTO.getBreed());
            if (updatePetDTO.getSpecies() != null)        pet.setSpecies(updatePetDTO.getSpecies());
            if (updatePetDTO.getMyVaccinesURLs() != null) pet.setMyVaccinesURLs(updatePetDTO.getMyVaccinesURLs());
            if (updatePetDTO.getMyPicturesURLs() != null) pet.setMyPicturesURLs(updatePetDTO.getMyPicturesURLs());
        }

        post = petPostRepository.save(post);
        return dtoConversionService.mapToPetPostDTO(post);
    }

    @Override
    public void deletePetPostById(UUID postId, UUID userId) {
        PetPost post = petPostRepository.findById(postId)
                .orElseThrow(() -> new PetPostNotFound("Pet post not found: " + postId));

        if (!post.getOwner().getUserId().equals(userId)) {
            throw new UserAccessDenied("You can only delete your own posts");
        }

        petPostRepository.deleteById(postId);
    }

    @Override
    public PetPostDTO toggleReact(UUID postId, UUID userId) {
        PetPost post = petPostRepository.findById(postId)
                .orElseThrow(() -> new PetPostNotFound("Pet post not found with ID: " + postId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFound("User not found with ID: " + userId));

        Set<User> reactedUsers = post.getReactedUsers();
        int reacts = post.getReacts();
        if (reactedUsers.contains(user)) {
            reactedUsers.remove(user);
            reacts --;
        }
        else {
            reactedUsers.add(user);
            reacts ++;
        }
        post.setReacts(reacts);
        post = petPostRepository.save(post);
        return dtoConversionService.mapToPetPostDTO(post);
    }

    @Override
    public Page<PetPostDTO> getFilteredPosts(PetPostFilterDTO filter, int page, int size) {
        Specification<PetPost> spec = PetPostSpecification.withFilters(filter);

        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSortOrder()),
                filter.getSortBy().equals("likes") ? "reacts" : "createdAt");

        Pageable pageable = PageRequest.of(page, size, sort);

        return petPostRepository.findAll(spec, pageable)
                .map(dtoConversionService::mapToPetPostDTO);
    }
}
