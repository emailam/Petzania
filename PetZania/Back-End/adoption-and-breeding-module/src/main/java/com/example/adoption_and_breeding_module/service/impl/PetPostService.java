package com.example.adoption_and_breeding_module.service.impl;

import com.example.adoption_and_breeding_module.exception.BlockingExist;
import com.example.adoption_and_breeding_module.exception.PetPostNotFound;
import com.example.adoption_and_breeding_module.exception.UserAccessDenied;
import com.example.adoption_and_breeding_module.exception.UserNotFound;
import com.example.adoption_and_breeding_module.model.dto.*;
import com.example.adoption_and_breeding_module.model.entity.Pet;
import com.example.adoption_and_breeding_module.model.entity.PetPost;
import com.example.adoption_and_breeding_module.model.entity.User;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostSortBy;
import com.example.adoption_and_breeding_module.repository.BlockRepository;
import com.example.adoption_and_breeding_module.repository.PetPostRepository;
import com.example.adoption_and_breeding_module.repository.UserRepository;
import com.example.adoption_and_breeding_module.service.IDTOConversionService;
import com.example.adoption_and_breeding_module.service.IPetPostService;
import com.example.adoption_and_breeding_module.util.PetPostSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.example.adoption_and_breeding_module.model.enumeration.PetPostSortBy.CREATED_DATE;
import static com.example.adoption_and_breeding_module.model.enumeration.PetPostSortBy.REACTS;

@Service
@RequiredArgsConstructor
@Transactional
public class PetPostService implements IPetPostService {

    private final UserRepository userRepository;
    private final PetPostRepository petPostRepository;
    private final BlockRepository blockRepository;
    private final IDTOConversionService dtoConversionService;
    private final NotificationPublisher notificationPublisher;
    private final FeedScorer feedScorer;

    @Override
    public PetPostDTO createPetPost(CreatePetPostDTO dto, UUID ownerId){
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFound("User not found with id: " + ownerId));

        PetPost post = PetPost.builder()
                .pet(dtoConversionService.mapToPet(dto.getPetDTO()))
                .owner(owner)
                .description(dto.getDescription())
                .postType(dto.getPostType())
                .location(dto.getLocation())
                .score(0)
                .build();

        post = petPostRepository.save(post);

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
        if(dto.getLocation() != null) {
            post.setLocation(dto.getLocation());
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

        System.out.println("yarab ala2y el error");
        Set<User> reactedUsers = post.getReactedUsers();
        int reacts = post.getReacts();
        if (reactedUsers.contains(user)) {
            reactedUsers.remove(user);
            reacts--;
        } else {
            reactedUsers.add(user);
            notificationPublisher.sendPetPostLikedNotification(ownerId, userId, postId);
            reacts++;
        }
        post.setReacts(reacts);
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

        PetPostSortBy sortBy = filter.getSortBy();
        boolean descending = filter.isSortDesc();

        if (sortBy == PetPostSortBy.SCORE) {
            List<PetPost> posts = petPostRepository.findAll(spec);
            feedScorer.scoreAndSort(posts, userId);
            if (!descending) {
                Collections.reverse(posts);
            }

            int start = page * size;
            int end = Math.min(start + size, posts.size());

            List<PetPostDTO> pageContent = posts.subList(start, end)
                    .stream()
                    .map(dtoConversionService::mapToPetPostDTO)
                    .toList();

            return new PageImpl<>(pageContent, PageRequest.of(page, size), posts.size());
        }
        else {
            String sortField = switch (sortBy) {
                case REACTS -> "reacts";
                default -> "createdAt"; // fallback
            };

            Sort.Direction direction = descending ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

            return petPostRepository
                    .findAll(spec, pageable)
                    .map(dtoConversionService::mapToPetPostDTO);
        }
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
