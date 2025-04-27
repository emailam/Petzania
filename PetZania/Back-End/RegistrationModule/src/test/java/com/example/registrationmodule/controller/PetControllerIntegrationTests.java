package com.example.registrationmodule.controller;

import com.example.registrationmodule.TestDataUtil;
import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.Pet;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.service.IDTOConversionService;
import com.example.registrationmodule.service.IPetService;
import com.example.registrationmodule.service.IUserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
public class PetControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUserService userService;

    @Autowired
    private IPetService petService;

    @Autowired
    private IDTOConversionService dtoConversionService;

    @Autowired
    private ObjectMapper objectMapper;

    private final String DEFAULT_PASSWORD = "Password123#";
    private User testUserA;
    private String token;


    @BeforeEach
    public void init() throws Exception {
        testUserA = userService.saveUser(TestDataUtil.createTestUserA());
        token = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);
    }

    private String obtainAccessToken(String email, String password) throws Exception {
        String loginPayload = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", password
        ));

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(response);

        // Navigate to tokenDTO -> accessToken
        return "Bearer " + jsonNode.get("tokenDTO").get("accessToken").asText();
    }

    @Test
    public void testCreateValidPet() throws Exception {
        Pet testPetA = TestDataUtil.createTestPetA(testUserA);
        PetDTO testPetDTO = dtoConversionService.mapToPetDto(testPetA);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/pet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token)
                        .content(objectMapper.writeValueAsString(testPetDTO)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        UUID createdPetId = UUID.fromString(objectMapper.readTree(response).get("petId").asText());
        assertTrue(petService.existsById(createdPetId));
    }

    @Test
    public void testCreatePetUserNull() throws Exception {
        testUserA.setUserId(null);
        Pet testPetA = TestDataUtil.createTestPetA(testUserA);
        PetDTO testPetDTO = dtoConversionService.mapToPetDto(testPetA);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token)
                        .content(objectMapper.writeValueAsString(testPetDTO)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void testCreatePetUserNotFound() throws Exception {
        testUserA.setUserId(UUID.randomUUID());
        Pet testPetA = TestDataUtil.createTestPetA(testUserA);
        PetDTO testPetDTO = dtoConversionService.mapToPetDto(testPetA);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token)
                        .content(objectMapper.writeValueAsString(testPetDTO)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testGetValidPetById() throws Exception {
        Pet testPetA = petService.savePet(TestDataUtil.createTestPetA(testUserA));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/pet/{id}", testPetA.getPetId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.petId").value(testPetA.getPetId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(testPetA.getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.species").value(testPetA.getSpecies().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.age").value(dtoConversionService.getPetAge(testPetA.getDateOfBirth())));
    }

    @Test
    public void testGePetByIdNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/pet/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testGetAllPetsByValidUserIdNotEmpty() throws Exception {
        Pet testPetA = petService.savePet(TestDataUtil.createTestPetA(testUserA));
        Pet testPetB = petService.savePet(TestDataUtil.createTestPetA(testUserA));
        Pet testPetC = petService.savePet(TestDataUtil.createTestPetA(testUserA));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/{id}/pets", testUserA.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(3)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].petId").value(testPetA.getPetId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].petId").value(testPetB.getPetId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[2].petId").value(testPetC.getPetId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value(testPetA.getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].name").value(testPetB.getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[2].name").value(testPetC.getName()));
    }

    @Test
    public void testGetAllPetsByValidUserIdEmpty() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/{id}/pets", testUserA.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(0)));

    }

    @Test
    public void testGetAllPetsByInvalidUserId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/{id}/pets", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isNotFound());

    }

    @Test
    public void testUpdatePetByValidId() throws Exception {
        Pet testPetA = petService.savePet(TestDataUtil.createTestPetA(testUserA));
        UpdatePetDTO updatePetDTO = new UpdatePetDTO();
        updatePetDTO.setName("Koky");
        updatePetDTO.setDescription("Beshoy's cat");

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/pet/{id}", testPetA.getPetId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token)
                        .content(objectMapper.writeValueAsString(updatePetDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.petId").value(testPetA.getPetId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(updatePetDTO.getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.species").value(testPetA.getSpecies().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value(updatePetDTO.getDescription()));

        Pet updatedPet = petService.getPetById(testPetA.getPetId()).orElseThrow();
        assertEquals(updatePetDTO.getName(), updatedPet.getName());
        assertEquals(updatePetDTO.getDescription(), updatedPet.getDescription());
        assertEquals(testPetA.getSpecies(), updatedPet.getSpecies());
    }

    @Test
    public void testUpdatePetByInvalidId() throws Exception {
        UpdatePetDTO updatePetDTO = new UpdatePetDTO();

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/pet/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token)
                        .content(objectMapper.writeValueAsString(updatePetDTO)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testDeletePetByIdValid() throws Exception {
        Pet testPetA = petService.savePet(TestDataUtil.createTestPetA(testUserA));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/pet/{id}", testPetA.getPetId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        assertFalse(petService.existsById(testPetA.getPetId()));
    }

    @Test
    public void testDeletePetByIdInvalid() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/pet/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }
}