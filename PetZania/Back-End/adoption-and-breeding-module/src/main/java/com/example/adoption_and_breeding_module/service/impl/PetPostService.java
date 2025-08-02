package com.example.adoption_and_breeding_module.service.impl;

import com.example.adoption_and_breeding_module.exception.BlockingExist;
import com.example.adoption_and_breeding_module.exception.PetPostInterestNotFound;
import com.example.adoption_and_breeding_module.exception.PetPostNotFound;
import com.example.adoption_and_breeding_module.exception.UserAccessDenied;
import com.example.adoption_and_breeding_module.exception.UserNotFound;
import com.example.adoption_and_breeding_module.model.dto.*;
import com.example.adoption_and_breeding_module.model.entity.Pet;
import com.example.adoption_and_breeding_module.model.entity.PetPost;
import com.example.adoption_and_breeding_module.model.entity.User;
import com.example.adoption_and_breeding_module.model.entity.PetPostInterest;
import com.example.adoption_and_breeding_module.model.entity.PetPostInterestId;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostSortBy;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostType;
import com.example.adoption_and_breeding_module.model.enumeration.PetSpecies;
import com.example.adoption_and_breeding_module.model.enumeration.InterestType;
import com.example.adoption_and_breeding_module.repository.*;
import com.example.adoption_and_breeding_module.repository.projection.BreedScore;
import com.example.adoption_and_breeding_module.repository.projection.OwnerScore;
import com.example.adoption_and_breeding_module.repository.projection.PostTypeScore;
import com.example.adoption_and_breeding_module.repository.projection.SpeciesScore;
import com.example.adoption_and_breeding_module.service.IDTOConversionService;
import com.example.adoption_and_breeding_module.service.IPetPostService;
import com.example.adoption_and_breeding_module.util.PetPostSpecification;
import com.example.adoption_and_breeding_module.repository.PetPostRepository.SpeciesCount;
import com.example.adoption_and_breeding_module.repository.PetPostRepository.TypeCount;
import com.example.adoption_and_breeding_module.repository.PetPostInterestRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.adoption_and_breeding_module.model.enumeration.PetPostSortBy.CREATED_DATE;
import static com.example.adoption_and_breeding_module.model.enumeration.PetPostSortBy.REACTS;
import static java.util.stream.Collectors.toMap;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

@Service
@RequiredArgsConstructor
@Transactional
public class PetPostService implements IPetPostService {

    private final UserRepository userRepository;
    private final PetPostRepository petPostRepository;
    private final BlockRepository blockRepository;
    private final FollowRepository followRepository;
    private final FriendshipRepository friendshipRepository;
    private final IDTOConversionService dtoConversionService;
    private final NotificationPublisher notificationPublisher;
    private final FeedScorer feedScorer;
    private final PetPostInterestRepository petPostInterestRepository;

    @Value("${post.expiration-days:30}")
    private long expirationDays;

    @Override
    public PetPostDTO createPetPost(CreatePetPostDTO dto, UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFound("User not found with id: " + ownerId));

        PetPost post = PetPost.builder()
                .pet(dtoConversionService.mapToPet(dto.getPetDTO()))
                .owner(owner)
                .description(dto.getDescription())
                .postType(dto.getPostType())
                .latitude(dto.getLatitude() != null ? dto.getLatitude() : 0.0)
                .longitude(dto.getLongitude() != null ? dto.getLongitude() : 0.0)
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

        if (dto.getLatitude() != null) {
            post.setLatitude(dto.getLatitude());
        }

        if (dto.getLongitude() != null) {
            post.setLongitude(dto.getLongitude());
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFound("User not found with ID: " + userId));

        // 1. Base spec + blocks
        Specification<PetPost> baseSpec = PetPostSpecification.withFilters(filter, expirationDays)
                .and(buildBlockSpec(userId));

        // 2. Load social graph
        List<UUID> followeeIds = followRepository
                .findFollowed_UserIdByFollower_UserId(userId);
        List<UUID> friends = Stream
                .concat(
                        friendshipRepository.findUser2_UserIdByUser1_UserId(userId).stream(),
                        friendshipRepository.findUser1_UserIdByUser2_UserId(userId).stream()
                )
                .distinct()
                .toList();

        // 3. Prefetch affinity counts
        long userTotalReacts = petPostRepository.countByReactedUsersUserId(userId);
        Map<PetSpecies, Long> reactsBySpecies = petPostRepository.countReactsBySpecies(userId)
                .stream().collect(Collectors.toMap(
                        SpeciesCount::getSpecies,
                        SpeciesCount::getCnt
                ));
        Map<PetPostType, Long> reactsByType = petPostRepository.countReactsByPostType(userId)
                .stream().collect(Collectors.toMap(
                        TypeCount::getPostType,
                        TypeCount::getCnt
                ));
        // 3b. Fetch interest metrics

        List<SpeciesScore> speciesScores = petPostInterestRepository.scoreBySpecies(userId);
        Map<PetSpecies, Long> interestSpecies = speciesScores.stream()
                .collect(Collectors.toMap(SpeciesScore::getSpecies, SpeciesScore::getScore));

        List<BreedScore> breedScores = petPostInterestRepository.scoreByBreed(userId);
        Map<String, Long> interestBreed = breedScores.stream()
                .collect(Collectors.toMap(BreedScore::getBreed, BreedScore::getScore));

        List<PostTypeScore> postTypeScores = petPostInterestRepository.scoreByPostType(userId);
        Map<PetPostType, Long> interestPostType = postTypeScores.stream()
                .collect(Collectors.toMap(PostTypeScore::getPostType, PostTypeScore::getScore));

        List<OwnerScore> ownerScores = petPostInterestRepository.scoreByOwner(userId);
        Map<UUID, Long> interestOwner = ownerScores.stream()
                .collect(Collectors.toMap(OwnerScore::getOwnerId, OwnerScore::getScore));

        if (filter.getSortBy() == PetPostSortBy.SCORE) {
            int window = (page + 1) * size;
            Pageable recPage   = PageRequest.of(0, window, Sort.by(DESC, "createdAt"));
            Pageable popPage   = PageRequest.of(0, window, Sort.by(DESC, "reacts"));

            // Bucket 1: friends’ recency
            Specification<PetPost> friendsSpec = baseSpec.and((r, q, cb) ->
                    r.get("owner").get("userId").in(friends));
            List<PetPost> friendPosts = petPostRepository.findAll(friendsSpec, recPage).getContent();

            // Bucket 2: followees’ recency
            Specification<PetPost> followeeSpec = baseSpec.and((r, q, cb) ->
                    r.get("owner").get("userId").in(followeeIds));
            List<PetPost> followeePosts = petPostRepository.findAll(followeeSpec, recPage).getContent();

            // Bucket 3: interest recency (top‐N species/types)
            List<PetSpecies> topSpecies = topKeys(reactsBySpecies, 3);
            List<PetPostType> topTypes  = topKeys(reactsByType, 2);
            Specification<PetPost> interestSpec = baseSpec.and((r, q, cb) -> cb.or(
                    r.get("pet").get("species").in(topSpecies),
                    r.get("postType").in(topTypes)
            ));
            List<PetPost> interestPosts = petPostRepository.findAll(interestSpec, recPage).getContent();

            // Bucket 4: global popular
            List<PetPost> popularPosts = petPostRepository.findAll(baseSpec, popPage).getContent();

            // Union & dedupe
            Set<PetPost> union = new LinkedHashSet<>();
            union.addAll(friendPosts);
            union.addAll(followeePosts);
            union.addAll(interestPosts);
            union.addAll(popularPosts);
            List<PetPost> candidates = new ArrayList<>(union);

            // Score & sort with interest metrics
            feedScorer.scoreAndSort(
                    candidates,
                    user.getLatitude(),
                    user.getLongitude(),
                    userTotalReacts,
                    reactsBySpecies,
                    reactsByType,
                    friends,
                    followeeIds,
                    interestSpecies,
                    interestBreed,
                    interestPostType,
                    interestOwner
            );

            // Slice page
            int start = page * size, end = Math.min(start + size, candidates.size());
            List<PetPostDTO> dtos = candidates.subList(start, end).stream()
                    .map(dtoConversionService::mapToPetPostDTO)
                    .toList();

            return new PageImpl<>(dtos, PageRequest.of(page, size), candidates.size());
        }

        // fallback DB paging
        Sort.Direction dir = filter.isSortDesc() ? DESC : ASC;
        String field = filter.getSortBy() == REACTS ? "reacts" : "createdAt";
        return petPostRepository.findAll(baseSpec, PageRequest.of(page, size, Sort.by(dir, field)))
                .map(dtoConversionService::mapToPetPostDTO);
    }

    @Override
    public void markInterest(UUID postId, UUID userId, InterestType interestType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFound("User not found with ID: " + userId));
        PetPost post = petPostRepository.findById(postId)
                .orElseThrow(() -> new PetPostNotFound("Pet post not found with ID: " + postId));
        PetPostInterestId interestId = new PetPostInterestId(userId, postId);
        PetPostInterest interest = petPostInterestRepository.findById(interestId)
                .orElse(PetPostInterest.builder().id(interestId).user(user).post(post).build());
        interest.setInterestType(interestType);
        petPostInterestRepository.save(interest);
    }

    @Override
    public void removeInterest(UUID postId, UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFound("User not found with ID: " + userId);
        }
        if (!petPostRepository.existsById(postId)) {
            throw new PetPostNotFound("Pet post not found with ID: " + postId);
        }
        // throw exception if interest does not exist
        PetPostInterestId interestId = new PetPostInterestId(userId, postId);
        if (!petPostInterestRepository.existsById(interestId)) {
            throw new PetPostInterestNotFound("Pet post interest not found with ID: " + interestId);
        }
        petPostInterestRepository.deleteById(interestId);
    }

    /** Helper: pick the top N keys by their map values (descending). */
    private <K> List<K> topKeys(Map<K,Long> map, int n) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<K,Long>comparingByValue(Comparator.reverseOrder()))
                .limit(n)
                .map(Map.Entry::getKey)
                .toList();
    }

    private Specification<PetPost> buildBlockSpec(UUID userId) {
        // users I have blocked
        List<UUID> usersBlockedByMe = blockRepository.findByBlockerUserId(userId)
                .stream()
                .map(b -> b.getBlocked().getUserId())
                .toList();

        // users who have blocked me
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

        return blockSpec;
    }

    @Override
    public Page<PetPostDTO> getAllPetPostsByUserId(UUID requesterUserId, UUID userId, int page, int size) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFound("User not found with ID: " + userId);
        }
        if (blockRepository.existsByBlocker_UserIdAndBlocked_UserId(requesterUserId, userId) ||
                blockRepository.existsByBlocker_UserIdAndBlocked_UserId(userId, requesterUserId)) {
            throw new BlockingExist("Operation blocked due to existing block relationship");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(DESC, "createdAt"));
        Specification<PetPost> specByUser = (root, query, cb) ->
                cb.equal(root.get("owner").get("userId"), userId);
        Page<PetPost> posts = petPostRepository.findAll(specByUser, pageable);
        return posts.map(dtoConversionService::mapToPetPostDTO);
    }
}
