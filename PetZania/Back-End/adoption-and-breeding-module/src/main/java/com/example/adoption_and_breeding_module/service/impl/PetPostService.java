package com.example.adoption_and_breeding_module.service.impl;

import com.example.adoption_and_breeding_module.exception.BlockingExist;
import com.example.adoption_and_breeding_module.exception.PetPostNotFound;
import com.example.adoption_and_breeding_module.exception.UserAccessDenied;
import com.example.adoption_and_breeding_module.exception.UserNotFound;
import com.example.adoption_and_breeding_module.model.dto.*;
import com.example.adoption_and_breeding_module.model.entity.Block;
import com.example.adoption_and_breeding_module.model.entity.Pet;
import com.example.adoption_and_breeding_module.model.entity.PetPost;
import com.example.adoption_and_breeding_module.model.entity.User;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostType;
import com.example.adoption_and_breeding_module.repository.BlockRepository;
import com.example.adoption_and_breeding_module.repository.PetPostRepository;
import com.example.adoption_and_breeding_module.repository.UserRepository;
import com.example.adoption_and_breeding_module.service.IDTOConversionService;
import com.example.adoption_and_breeding_module.service.IPetPostService;
import com.example.adoption_and_breeding_module.util.PetPostSpecification;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PetPostService implements IPetPostService {

    private final UserRepository userRepository;
    private final PetPostRepository petPostRepository;
    private final BlockRepository blockRepository;
    private final IDTOConversionService dtoConversionService;
//    private final ToxicityChecker toxicityChecker;

    @Override
    public PetPostDTO createPetPost(CreatePetPostDTO dto, UUID ownerId) throws Exception {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFound("User not found with id: " + ownerId));

        PetPost post = PetPost.builder()
                .pet(dtoConversionService.mapToPet(dto.getPetDTO()))
                .owner(owner)
                .description(dto.getDescription())
                .postType(dto.getPostType())
                .location(dto.getLocation())
                .build();

        post = petPostRepository.save(post);

//        {
//            if (toxicityChecker.isToxic(dto.getDescription())) {
//                System.out.println("toxic");
//            }
//            else {
//                System.out.println("not toxic");
//            }
//        }


        return dtoConversionService.mapToPetPostDTO(post);
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
            if (updatePetDTO.getName() != null) pet.setName(updatePetDTO.getName());
            if (updatePetDTO.getDescription() != null) pet.setDescription(updatePetDTO.getDescription());
            if (updatePetDTO.getGender() != null) pet.setGender(updatePetDTO.getGender());
            if (updatePetDTO.getBreed() != null) pet.setBreed(updatePetDTO.getBreed());
            if (updatePetDTO.getSpecies() != null) pet.setSpecies(updatePetDTO.getSpecies());
            if (updatePetDTO.getMyVaccinesURLs() != null) pet.setMyVaccinesURLs(updatePetDTO.getMyVaccinesURLs());
            if (updatePetDTO.getMyPicturesURLs() != null) pet.setMyPicturesURLs(updatePetDTO.getMyPicturesURLs());
        }
        post.setUpdatedAt(Instant.now());
//        post = petPostRepository.save(post);
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

        UUID ownerId = post.getOwner().getUserId();

        if (blockRepository.existsByBlocker_UserIdAndBlocked_UserId(ownerId, userId) ||
                blockRepository.existsByBlocker_UserIdAndBlocked_UserId(userId, ownerId)) {
            throw new BlockingExist("Operation blocked due to existing block relationship");
        }

        Set<User> reactedUsers = post.getReactedUsers();
        int reacts = post.getReacts();
        if (reactedUsers.contains(user)) {
            reactedUsers.remove(user);
            reacts--;
        } else {
            reactedUsers.add(user);
            reacts++;
        }
        post.setReacts(reacts);
//        System.out.println(post);
//        petPostRepository.save(post);
        return dtoConversionService.mapToPetPostDTO(post);
    }

    @Override
    public Page<PetPostDTO> getFilteredPosts(
            UUID userId,
            PetPostFilterDTO filter,
            int page,
            int size
    ) {
        Specification<PetPost> spec = PetPostSpecification.withFilters(filter);

        List<UUID> usersBlockedByMe = blockRepository.findByBlockerUserId(userId)
                .stream()
                .map(b -> b.getBlocked().getUserId())
                .toList();
        List<UUID> usersBlockingMe = blockRepository.findByBlockedUserId(userId)
                .stream()
                .map(b -> b.getBlocker().getUserId())
                .toList();

        Specification<PetPost> blockSpec = (root, query, cb) -> cb.conjunction();

        if (!usersBlockedByMe.isEmpty()) {
            blockSpec = blockSpec.and((root, query, cb) ->
                    cb.not(root.get("owner").get("userId").in(usersBlockedByMe))
            );
        }
        if (!usersBlockingMe.isEmpty()) {
            blockSpec = blockSpec.and((root, query, cb) ->
                    cb.not(root.get("owner").get("userId").in(usersBlockingMe))
            );
        }

        spec = spec.and(blockSpec);

        Sort sort = Sort.by(
                Sort.Direction.fromString(filter.getSortOrder()),
                filter.getSortBy().equals("likes") ? "reacts" : "createdAt"
        );
        Pageable pageable = PageRequest.of(page, size, sort);

        return petPostRepository
                .findAll(spec, pageable)
                .map(dtoConversionService::mapToPetPostDTO);
    }


    @Override
    public Page<PetPostDTO> getAllPetPostsByUserId(UUID requesterUserId, UUID userId, int page, int size) {
        if (blockRepository.existsByBlocker_UserIdAndBlocked_UserId(requesterUserId, userId) ||
                blockRepository.existsByBlocker_UserIdAndBlocked_UserId(userId, requesterUserId)) {
            throw new BlockingExist("Operation blocked due to existing block relationship");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<PetPost> specByUser = (root, query, cb) ->
                cb.equal(root.get("owner").get("userId"), userId);
        Page<PetPost> posts = petPostRepository.findAll(specByUser, pageable);
        return posts.map(dtoConversionService::mapToPetPostDTO);
    }
}
