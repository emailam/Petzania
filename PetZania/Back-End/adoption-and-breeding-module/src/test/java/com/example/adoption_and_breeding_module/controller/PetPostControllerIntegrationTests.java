package com.example.adoption_and_breeding_module.controller;


import com.example.adoption_and_breeding_module.TestDataUtil;
import com.example.adoption_and_breeding_module.model.dto.*;
import com.example.adoption_and_breeding_module.model.entity.Block;
import com.example.adoption_and_breeding_module.model.entity.Pet;
import com.example.adoption_and_breeding_module.model.entity.PetPost;
import com.example.adoption_and_breeding_module.model.entity.User;
import com.example.adoption_and_breeding_module.model.enumeration.Gender;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostStatus;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostType;
import com.example.adoption_and_breeding_module.model.enumeration.PetSpecies;
import com.example.adoption_and_breeding_module.model.principal.UserPrincipal;
import com.example.adoption_and_breeding_module.repository.BlockRepository;
import com.example.adoption_and_breeding_module.repository.PetPostRepository;
import com.example.adoption_and_breeding_module.repository.UserRepository;
import com.example.adoption_and_breeding_module.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import static org.hamcrest.Matchers.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
public class PetPostControllerIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PetPostRepository petPostRepository;

    @Autowired
    private BlockRepository blockRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private User userA;
    private User userB;
    private User userC;
    private PetPost adoptionPost;
    private PetPost breedingPost;
    private Pet testPet;

    @BeforeEach
    void setup() {
        // Clean up first
        petPostRepository.deleteAll();
        blockRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        userA = userRepository.save(TestDataUtil.createTestUser("userA"));
        userB = userRepository.save(TestDataUtil.createTestUser("userB"));
        userC = userRepository.save(TestDataUtil.createTestUser("userC"));

        // Set up security context for userA by default
        UserPrincipal userPrincipal = new UserPrincipal(userA);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userPrincipal,
                        null,
                        userPrincipal.getAuthorities()
                )
        );

        // Create test pet
        testPet = TestDataUtil.createTestPet("Buddy", PetSpecies.DOG, Gender.MALE, 24); // 2 years old

        // Create test posts
        adoptionPost = petPostRepository.save(PetPost.builder()
                .owner(userA)
                .pet(testPet)
                .postType(PetPostType.ADOPTION)
                .postStatus(PetPostStatus.PENDING)
                .description("Looking for a loving home")
                .location("New York")
                .reacts(0)
                .reactedUsers(new HashSet<>())
                .build());

        breedingPost = petPostRepository.save(PetPost.builder()
                .owner(userB)
                .pet(TestDataUtil.createTestPet("Luna", PetSpecies.CAT, Gender.FEMALE, 36)) // 3 years old
                .postType(PetPostType.BREEDING)
                .postStatus(PetPostStatus.PENDING)
                .description("Purebred Persian cat for breeding")
                .location("Los Angeles")
                .reacts(1)
                .reactedUsers(new HashSet<>(Set.of(userA)))
                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPetPost_Success() throws Exception {
        PetDTO petDTO = TestDataUtil.createPetDTO("Max", PetSpecies.DOG, Gender.MALE);

        setupSecurityContext(userA);
        System.out.println(SecurityUtils.getCurrentUser().getUser());
        CreatePetPostDTO createDTO = new CreatePetPostDTO();
        createDTO.setPetDTO(petDTO);
        createDTO.setDescription("Friendly dog needs home");
        createDTO.setPostType(PetPostType.ADOPTION);
        createDTO.setLocation("Brazil");

        mockMvc.perform(post("/api/pet-posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.petDTO.name").value("Max"))
                .andExpect(jsonPath("$.petDTO.age").exists())
                .andExpect(jsonPath("$.description").value("Friendly dog needs home"))
                .andExpect(jsonPath("$.postType").value("ADOPTION"))
                .andExpect(jsonPath("$.postStatus").value("PENDING"))
                .andExpect(jsonPath("$.reacts").value(0))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.location").exists()); // Location should be set by service

        // Verify post was saved
        assertEquals(3, petPostRepository.count());
    }

    @Test
    void createPetPost_ValidationErrors() throws Exception {
        // Test missing required fields
        CreatePetPostDTO createDTO = new CreatePetPostDTO();

        mockMvc.perform(post("/api/pet-posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.status").value(406))
                .andExpect(jsonPath("$.error").value("Not Acceptable"))
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message[*].field").value(hasItems("petDTO", "postType")))
                .andExpect(jsonPath("$.message[*].message").value(hasItems("Pet DTO is required.", "Post type is required.")));
    }

    @Test
    void createPetPost_InvalidEnumValue() throws Exception {
        String invalidJson = """
                {
                    "petDTO": {
                        "name": "Test",
                        "gender": "INVALID_GENDER",
                        "dateOfBirth": "2022-01-01",
                        "breed": "Test Breed",
                        "species": "DOG"
                    },
                    "postType": "INVALID_TYPE"
                }
                """;

        mockMvc.perform(post("/api/pet-posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPetPost_NoAuthentication() throws Exception {
        SecurityContextHolder.clearContext();

        PetDTO petDTO = TestDataUtil.createPetDTO("Max", PetSpecies.DOG, Gender.MALE);
        CreatePetPostDTO createDTO = new CreatePetPostDTO();
        createDTO.setPetDTO(petDTO);
        createDTO.setPostType(PetPostType.ADOPTION);
        createDTO.setLocation("China");

        mockMvc.perform(post("/api/pet-posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest()); // AuthenticatedUserNotFound
    }

    @Test
    void getPetPostById_Success() throws Exception {
        mockMvc.perform(get("/api/pet-posts/{petPostId}", adoptionPost.getPostId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(adoptionPost.getPostId().toString()))
                .andExpect(jsonPath("$.petDTO.name").value("Buddy"))
                .andExpect(jsonPath("$.petDTO.petId").value(testPet.getPetId().toString()))
                .andExpect(jsonPath("$.petDTO.age").value("2 years"))
                .andExpect(jsonPath("$.postType").value("ADOPTION"))
                .andExpect(jsonPath("$.location").value("New York"));
    }

    @Test
    void getPetPostById_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/pet-posts/{petPostId}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Pet post not found with id: " + nonExistentId));
    }

    @Test
    void getAllPetPostsByUserId_Success() throws Exception {
        mockMvc.perform(get("/api/pet-posts/user/{userId}", userA.getUserId())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].ownerId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void getAllPetPostsByUserId_BlockedUser_Forbidden() throws Exception {
        // Create a block relationship
        blockRepository.save(Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userA)
                .blocked(userB)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build());

        mockMvc.perform(get("/api/pet-posts/user/{userId}", userB.getUserId())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Operation blocked due to existing block relationship"));
    }

    @Test
    void getFilteredPosts_CompleteFilterTest() throws Exception {
        // Create posts with different characteristics for filtering
        petPostRepository.saveAll(List.of(
                PetPost.builder()
                        .owner(userC)
                        .pet(TestDataUtil.createTestPet("Young Dog", PetSpecies.DOG, Gender.MALE, 6)) // 6 months
                        .postType(PetPostType.ADOPTION)
                        .postStatus(PetPostStatus.PENDING)
                        .location("Chicago")
                        .reacts(5)
                        .build(),
                PetPost.builder()
                        .owner(userC)
                        .pet(TestDataUtil.createTestPet("Old Cat", PetSpecies.CAT, Gender.FEMALE, 120)) // 10 years
                        .postType(PetPostType.BREEDING)
                        .postStatus(PetPostStatus.PENDING)
                        .location("Miami")
                        .reacts(10)
                        .build()
        ));

        // Test with all filter parameters
        PetPostFilterDTO filter = new PetPostFilterDTO();
        filter.setPetPostType(PetPostType.ADOPTION);
        filter.setSpecies(PetSpecies.DOG);
        filter.setGender(Gender.MALE);
        filter.setBreed("Test"); // Partial match
        filter.setMinAge(5);
        filter.setMaxAge(30);
        filter.setSortBy("likes");
        filter.setSortOrder("desc");

        mockMvc.perform(get("/api/pet-posts/filtered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2))) // Buddy (24 months) and Young Dog (6 months)
                .andExpect(jsonPath("$.content[0].reacts").value(5)) // Sorted by likes desc
                .andExpect(jsonPath("$.content[1].reacts").value(0));
    }

    @Test
    void getFilteredPosts_WithBlockedUsers() throws Exception {
        // Block userB
        blockRepository.save(Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userA)
                .blocked(userB)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build());

        PetPostFilterDTO filter = new PetPostFilterDTO();
        filter.setSortBy("date");
        filter.setSortOrder("desc");

        mockMvc.perform(get("/api/pet-posts/filtered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1))) // Only userA's post visible
                .andExpect(jsonPath("$.content[0].ownerId").value(userA.getUserId().toString()));
    }

    @Test
    void updatePetPost_PartialUpdate() throws Exception {
        // Test updating only post description
        UpdatePetPostDTO updateDTO = new UpdatePetPostDTO();
        updateDTO.setDescription("Only updating description");

        mockMvc.perform(patch("/api/pet-posts/{petPostId}", adoptionPost.getPostId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Only updating description"))
                .andExpect(jsonPath("$.petDTO.name").value("Buddy")) // Unchanged
                .andExpect(jsonPath("$.postStatus").value("PENDING")); // Unchanged
    }

    @Test
    void updatePetPost_CompleteUpdate() throws Exception {
        UpdatePetDTO updatePetDTO = new UpdatePetDTO();
        updatePetDTO.setName("Buddy Updated");
        updatePetDTO.setDescription("Updated pet description");
        updatePetDTO.setBreed("Golden Retriever");
        updatePetDTO.setMyVaccinesURLs(List.of("new-vaccine1.jpg", "new-vaccine2.jpg"));
        updatePetDTO.setMyPicturesURLs(List.of("new-pic1.jpg", "new-pic2.jpg"));

        UpdatePetPostDTO updateDTO = new UpdatePetPostDTO();
        updateDTO.setUpdatePetDTO(updatePetDTO);
        updateDTO.setDescription("Updated post description");
        updateDTO.setPostStatus(PetPostStatus.COMPLETED);

        mockMvc.perform(patch("/api/pet-posts/{petPostId}", adoptionPost.getPostId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.petDTO.name").value("Buddy Updated"))
                .andExpect(jsonPath("$.petDTO.description").value("Updated pet description"))
                .andExpect(jsonPath("$.petDTO.breed").value("Golden Retriever"))
                .andExpect(jsonPath("$.petDTO.myVaccinesURLs", hasSize(2)))
                .andExpect(jsonPath("$.description").value("Updated post description"))
                .andExpect(jsonPath("$.postStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void updatePetPost_NotOwner_Forbidden() throws Exception {
        setupSecurityContext(userB);

        UpdatePetPostDTO updateDTO = new UpdatePetPostDTO();
        updateDTO.setDescription("Trying to update someone else's post");

        mockMvc.perform(patch("/api/pet-posts/{petPostId}", adoptionPost.getPostId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You can only update your own posts"));
    }

    @Test
    void deletePetPost_Success() throws Exception {
        UUID postId = adoptionPost.getPostId();

        mockMvc.perform(delete("/api/pet-posts/{petPostId}", postId))
                .andExpect(status().isNoContent());

        // Verify post was deleted
        assertFalse(petPostRepository.existsById(postId));
        assertEquals(1, petPostRepository.count()); // Only breedingPost remains
    }

    @Test
    void deletePetPost_NotOwner_Forbidden() throws Exception {
        setupSecurityContext(userB);

        mockMvc.perform(delete("/api/pet-posts/{petPostId}", adoptionPost.getPostId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You can only delete your own posts"));

        // Verify post was NOT deleted
        assertTrue(petPostRepository.existsById(adoptionPost.getPostId()));
    }

    @Test
    void deletePetPost_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/pet-posts/{petPostId}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Pet post not found: " + nonExistentId));
    }

    @Test
    void toggleReact_AddReaction_Success() throws Exception {
        setupSecurityContext(userB);

        mockMvc.perform(put("/api/pet-posts/{petPostId}/react", adoptionPost.getPostId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reacts").value(1))
                .andExpect(jsonPath("$.reactedUsersIds", hasSize(1)))
                .andExpect(jsonPath("$.reactedUsersIds", hasItem(userB.getUserId().toString())));

        // Verify in database
        PetPost updated = petPostRepository.findById(adoptionPost.getPostId()).orElseThrow();
        assertEquals(1, updated.getReacts());
        assertTrue(updated.getReactedUsers().stream()
                .anyMatch(user -> user.getUserId().equals(userB.getUserId())));
    }

    @Test
    void toggleReact_RemoveReaction_Success() throws Exception {
        // UserA already reacted to breedingPost in setup
        setupSecurityContext(userA);
        mockMvc.perform(put("/api/pet-posts/{petPostId}/react", breedingPost.getPostId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reacts").value(0))
                .andExpect(jsonPath("$.reactedUsersIds", empty()));

        // Verify in database
        PetPost updated = petPostRepository.findById(breedingPost.getPostId()).orElseThrow();
        assertEquals(0, updated.getReacts());
        assertTrue(updated.getReactedUsers().isEmpty());
    }

    @Test
    void toggleReact_MultipleUsersReacting() throws Exception {
        // First user reacts
        setupSecurityContext(userB);
        mockMvc.perform(put("/api/pet-posts/{petPostId}/react", adoptionPost.getPostId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reacts").value(1));

        // Second user reacts
        setupSecurityContext(userC);
        mockMvc.perform(put("/api/pet-posts/{petPostId}/react", adoptionPost.getPostId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reacts").value(2))
                .andExpect(jsonPath("$.reactedUsersIds", hasSize(2)))
                .andExpect(jsonPath("$.reactedUsersIds", hasItems(
                        userB.getUserId().toString(),
                        userC.getUserId().toString()
                )));
    }

    @Test
    void toggleReact_BlockedByPostOwner_Forbidden() throws Exception {
        // Owner blocks user
        blockRepository.save(Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userA) // Post owner
                .blocked(userB)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build());

        setupSecurityContext(userB);

        mockMvc.perform(put("/api/pet-posts/{petPostId}/react", adoptionPost.getPostId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Operation blocked due to existing block relationship"));
    }

    @Test
    void toggleReact_UserBlockedPostOwner_Forbidden() throws Exception {
        // User blocks post owner
        blockRepository.save(Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userB)
                .blocked(userA) // Post owner
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build());

        setupSecurityContext(userB);

        mockMvc.perform(put("/api/pet-posts/{petPostId}/react", adoptionPost.getPostId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Operation blocked due to existing block relationship"));
    }

    @Test
    void toggleReact_UserNotFound() throws Exception {
        // Delete the current user to simulate user not found
        userRepository.delete(userA);

        mockMvc.perform(put("/api/pet-posts/{petPostId}/react", breedingPost.getPostId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found with ID: " + userA.getUserId()));
    }

    @Test
    void testPaginationAndSorting() throws Exception {
        // Create multiple posts with different react counts
        List<PetPost> posts = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            posts.add(petPostRepository.save(PetPost.builder()
                    .owner(userA)
                    .pet(TestDataUtil.createTestPet("Pet" + i, PetSpecies.DOG, Gender.MALE, 24))
                    .postType(PetPostType.ADOPTION)
                    .postStatus(PetPostStatus.PENDING)
                    .description("Test post " + i)
                    .location("Test Location " + i)
                    .reacts(i % 5) // Different react counts for sorting
                    .reactedUsers(new HashSet<>())
                    .build()));
        }

        // Test first page
        mockMvc.perform(get("/api/pet-posts/user/{userId}", userA.getUserId())
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(26)) // 25 new + 1 from setup
                .andExpect(jsonPath("$.totalPages").value(6))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        // Test last page
        mockMvc.perform(get("/api/pet-posts/user/{userId}", userA.getUserId())
                        .param("page", "5")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true));

        // Test sorting by likes descending
        PetPostFilterDTO filterLikes = new PetPostFilterDTO();
        filterLikes.setSortBy("likes");
        filterLikes.setSortOrder("desc");

        mockMvc.perform(get("/api/pet-posts/filtered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filterLikes))
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reacts").value(4))
                .andExpect(jsonPath("$.content[1].reacts").value(4))
                .andExpect(jsonPath("$.content[2].reacts").value(4));

        // Test sorting by date ascending
        PetPostFilterDTO filterDate = new PetPostFilterDTO();
        filterDate.setSortBy("date");
        filterDate.setSortOrder("asc");

        mockMvc.perform(get("/api/pet-posts/filtered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filterDate))
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].postId").value(adoptionPost.getPostId().toString())); // Oldest post
    }

    @Test
    void testInvalidUUIDFormat() throws Exception {
        mockMvc.perform(get("/api/pet-posts/{petPostId}", "invalid-uuid"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/pet-posts/user/{userId}", "invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSpecialCharactersInData() throws Exception {
        PetDTO petDTO = TestDataUtil.createPetDTO("Max & Buddy's Friend", PetSpecies.DOG, Gender.MALE);
        petDTO.setBreed("Côte d'Azur Shepherd");
        petDTO.setDescription("Special chars: <>&\"'{}[]");

        CreatePetPostDTO createDTO = new CreatePetPostDTO();
        createDTO.setPetDTO(petDTO);
        createDTO.setDescription("Test with émojis 🐕 and special chars: <script>alert('test')</script>");
        createDTO.setPostType(PetPostType.ADOPTION);
        createDTO.setLocation("China");

        mockMvc.perform(post("/api/pet-posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.petDTO.name").value("Max & Buddy's Friend"))
                .andExpect(jsonPath("$.petDTO.breed").value("Côte d'Azur Shepherd"));
    }


    private void setupSecurityContext(User user) {
        UserPrincipal userPrincipal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userPrincipal,
                        null,
                        userPrincipal.getAuthorities()
                )
        );
    }

    @Test
    void createPetPost_ToxicPetName_ShouldFailValidation() throws Exception {
        PetDTO petDTO = PetDTO.builder()
                .name("stupid dog") // toxic
                .description("friendly and playful")
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.of(2020, 5, 15))
                .age("4 years")
                .breed("Labrador")
                .species(PetSpecies.DOG)
                .build();

        CreatePetPostDTO dto = CreatePetPostDTO.builder()
                .petDTO(petDTO)
                .location("Cairo")
                .description("Nice post")
                .postType(PetPostType.ADOPTION)
                .build();

        mockMvc.perform(post("/api/pet-posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.message[0].field").value("petDTO.name"))
                .andExpect(jsonPath("$.message[0].message").value("Toxic content is not allowed."));
    }

    @Test
    void createPetPost_ToxicDescription_ShouldFailValidation() throws Exception {
        PetDTO petDTO = PetDTO.builder()
                .name("Max")
                .description("You are horrible") // toxic
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.of(2020, 5, 15))
                .age("4 years")
                .breed("Labrador")
                .species(PetSpecies.DOG)
                .build();

        CreatePetPostDTO dto = CreatePetPostDTO.builder()
                .petDTO(petDTO)
                .location("Alexandria")
                .description("Great pet")
                .postType(PetPostType.ADOPTION)
                .build();

        mockMvc.perform(post("/api/pet-posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.message[0].field").value("petDTO.description"))
                .andExpect(jsonPath("$.message[0].message").value("Toxic content is not allowed."));
    }

    @Test
    void updatePetPost_ToxicPostDescription_ShouldFail() throws Exception {
        UpdatePetPostDTO updateDTO = new UpdatePetPostDTO();
        updateDTO.setDescription("You're disgusting"); // toxic content

        mockMvc.perform(patch("/api/pet-posts/{petPostId}", adoptionPost.getPostId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.message[0].field").value("description"))
                .andExpect(jsonPath("$.message[0].message").value("Toxic content is not allowed."));
    }

    @Test
    void updatePetPost_ToxicPetName_ShouldFail() throws Exception {
        UpdatePetDTO petDTO = UpdatePetDTO.builder()
                .name("You're ugly") // toxic
                .build();

        UpdatePetPostDTO updateDTO = UpdatePetPostDTO.builder()
                .updatePetDTO(petDTO)
                .build();

        mockMvc.perform(patch("/api/pet-posts/{petPostId}", adoptionPost.getPostId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.message[0].field").value("updatePetDTO.name"))
                .andExpect(jsonPath("$.message[0].message").value("Toxic content is not allowed."));
    }

    @Test
    void updatePetPost_NotToxicEdgeCasePostDescription_ShouldPass() throws Exception {
        UpdatePetDTO petDTO = UpdatePetDTO.builder()
                .breed("Trash dog")        // toxic
                .description("Stupid pet") // toxic
                .build();

        UpdatePetPostDTO updateDTO = UpdatePetPostDTO.builder()
                .updatePetDTO(petDTO)
                .build();

        mockMvc.perform(patch("/api/pet-posts/{petPostId}", adoptionPost.getPostId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.message", hasSize(2)))
                .andExpect(jsonPath("$.message[?(@.field=='updatePetDTO.breed')].message").value(hasItem("Toxic content is not allowed.")))
                .andExpect(jsonPath("$.message[?(@.field=='updatePetDTO.description')].message").value(hasItem("Toxic content is not allowed.")));
    }

    @Test
    void createPetPost_NotToxicEdgeCaseDescription_ShouldPass() throws Exception {
        PetDTO petDTO = PetDTO.builder()
                .name("Max")
                .description("Great dog") // toxic
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.of(2020, 5, 15))
                .age("4 years")
                .breed("Labrador")
                .species(PetSpecies.DOG)
                .build();

        CreatePetPostDTO dto = CreatePetPostDTO.builder()
                .petDTO(petDTO)
                .location("Alexandria")
                .description("My dog had a horrible accident and needs adoption.")
                .postType(PetPostType.ADOPTION)
                .build();

        mockMvc.perform(post("/api/pet-posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }
}
