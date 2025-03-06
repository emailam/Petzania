package com.example.registrationmodule.controller;

import com.example.registrationmodule.TestDataUtil;
import com.example.registrationmodule.model.dto.UpdateUserProfileDto;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.service.IUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.UUID;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
public class PetControllerIntegrationTests {

    private MockMvc mockMvc;
    private IUserService userService;
    private ObjectMapper objectMapper;

    @Autowired
    public PetControllerIntegrationTests(MockMvc mockMvc, IUserService userService, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @Test
    public void testGetUserById_UserExists_ShouldReturnUserProfile() throws Exception {

        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/pet/user/{userId}", testUserA.getUserId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.username").value(testUserA.getUsername()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(testUserA.getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.bio").value(testUserA.getBio()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.profilePictureURL").value(testUserA.getProfilePictureURL()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.phoneNumber").value(testUserA.getPhoneNumber()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.userRoles").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.myPets").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.friendsCount").value(testUserA.getFriends().size()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.followersCount").value(testUserA.getFollowers().size()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.followingCount").value(testUserA.getFollowing().size()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.storeProfileId").
                        value(testUserA.getStoreProfileId() != null ? testUserA.getStoreProfileId().toString() : null))
                .andExpect(MockMvcResultMatchers.jsonPath("$.vetProfileId").
                        value(testUserA.getVetProfileId() != null ? testUserA.getVetProfileId().toString() : null));
    }


    @Test
    public void testGetUserById_UserNotFound_ShouldReturnNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(MockMvcRequestBuilders.get("/api/pet/user/{userId}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testUpdateUserProfileById_UserExists_ShouldUpdateAndReturnUserProfile() throws Exception {

        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
        UpdateUserProfileDto updateDto = new UpdateUserProfileDto();
        updateDto.setName("Updated Name");
        updateDto.setBio("Updated Bio");
        updateDto.setProfilePictureURL("https://example.com/new-profile.jpg");
        updateDto.setPhoneNumber("123456789");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/pet/user/{id}", testUserA.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(updateDto.getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.bio").value(updateDto.getBio()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.profilePictureURL").value(updateDto.getProfilePictureURL()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.phoneNumber").value(updateDto.getPhoneNumber()));
    }

    @Test
    public void testUpdateUserProfileById_UserNotFound_ShouldReturnNotFound() throws Exception {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        UpdateUserProfileDto updateDto = new UpdateUserProfileDto();
        updateDto.setName("Updated Name");

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/pet/user/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testUpdateUserProfileById_PartialUpdate_ShouldReturnUpdatedProfile() throws Exception {
        // Arrange
        User testUser = userService.saveUser(TestDataUtil.createTestUserA());
        UpdateUserProfileDto updateDto = new UpdateUserProfileDto();
        updateDto.setBio("Updated Bio Only");

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/pet/user/{id}", testUser.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.bio").value(updateDto.getBio()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(testUser.getName()));
    }
}
