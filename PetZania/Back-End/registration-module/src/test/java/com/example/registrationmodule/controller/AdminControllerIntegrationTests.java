package com.example.registrationmodule.controller;

import com.example.registrationmodule.TestDataUtil;
import com.example.registrationmodule.model.dto.AdminDTO;
import com.example.registrationmodule.model.dto.AdminLogoutDTO;
import com.example.registrationmodule.model.dto.LoginAdminDTO;
import com.example.registrationmodule.model.dto.RefreshTokenDTO;
import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.model.enumeration.AdminRole;
import com.example.registrationmodule.service.IAdminService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
public class AdminControllerIntegrationTests {
    private MockMvc mockMvc;
    private IAdminService adminService;
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    private final String DEFAULT_PASSWORD = "Password123#";
    private String superAdminToken;
    private String adminToken;

    @Autowired
    public AdminControllerIntegrationTests(MockMvc mockMvc, IAdminService adminService, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.adminService = adminService;
        this.objectMapper = objectMapper;
    }

    @BeforeEach
    public void setup() throws Exception {
        Admin superAdmin = TestDataUtil.createSuperAdmin("superAdminA");
        adminService.saveAdmin(superAdmin);
        superAdminToken = obtainAdminToken(superAdmin.getUsername(), DEFAULT_PASSWORD);

        Admin admin = TestDataUtil.createAdmin("adminA");
        adminService.saveAdmin(admin);
        adminToken = obtainAdminToken(admin.getUsername(), DEFAULT_PASSWORD);
    }
    @AfterEach
    public void cleanup(){
        Set<String> rateLimitKeys = redisTemplate.keys("rate_limit:*");
        if (!rateLimitKeys.isEmpty()) {
            redisTemplate.delete(rateLimitKeys);
        }
    }

    @Test
    public void testCreateAdmin_ValidDataAsSuperAdmin_ShouldReturnCreated() throws Exception {
        AdminDTO adminDTO = new AdminDTO();
        UUID adminId = UUID.randomUUID();
        adminDTO.setAdminId(adminId);
        adminDTO.setUsername("newAdminUser");
        adminDTO.setPassword("ValidPass123!");
        adminDTO.setAdminRole(AdminRole.ADMIN);


        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/create")
                        .header("Authorization", superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminDTO)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.username").value(adminDTO.getUsername()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.adminRole").value(adminDTO.getAdminRole().toString()));
    }

    @Test
    public void testCreateAdmin_InvalidPasswordFormat_ShouldReturnNotAcceptable() throws Exception {
        AdminDTO adminDTO = new AdminDTO();
        adminDTO.setUsername("newAdmin");
        adminDTO.setPassword("weak"); // Doesn't meet complexity requirements
        adminDTO.setAdminRole(AdminRole.ADMIN);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/create")
                        .header("Authorization", superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminDTO)))
                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());
    }
    @Test
    public void testCreateAdmin_MissingRequiredFields_ShouldReturnNotAcceptable() throws Exception {
        AdminDTO adminDTO = new AdminDTO();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/create")
                        .header("Authorization", superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminDTO)))
                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());
    }
    @Test
    public void testCreateAdmin_AsRegularAdmin_ShouldReturnForbidden() throws Exception {
        AdminDTO adminDTO = new AdminDTO();
        adminDTO.setUsername("newAdminUser");
        adminDTO.setPassword("ValidPass123!");
        adminDTO.setAdminRole(AdminRole.ADMIN);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/create")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminDTO)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    public void testGetAdminById_AdminNotFound_ShouldReturnNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/{id}", nonExistentId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testDeleteAdminById_AdminNotFound_ShouldReturnNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/admin/delete/{id}", nonExistentId)
                        .header("Authorization", superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testCreateAdmin_Unauthenticated_ShouldReturnUnauthorized() throws Exception {
        AdminDTO adminDTO = new AdminDTO();
        adminDTO.setUsername("newAdminUser");
        adminDTO.setPassword("ValidPass123!");
        adminDTO.setAdminRole(AdminRole.ADMIN);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminDTO)))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    public void testCreateAdmin_UsernameAlreadyExists_ShouldReturnConflict() throws Exception {
        AdminDTO adminDTO = new AdminDTO();
        adminDTO.setUsername("adminA");
        adminDTO.setPassword("ValidPass123!");
        adminDTO.setAdminRole(AdminRole.ADMIN);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/create")
                        .header("Authorization", superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminDTO)))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.content().string(containsString("Username already exists")));
    }

    @Test
    public void testGetAdminById_AdminExists_ShouldReturnAdmin() throws Exception {
        Admin admin = TestDataUtil.createAdmin("adminB");
        Admin savedAdmin = adminService.saveAdmin(admin);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/{id}", savedAdmin.getAdminId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.adminId").value(savedAdmin.getAdminId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.username").value(savedAdmin.getUsername()));
    }

    @Test
    public void testGetAdminById_Unauthorized_ShouldReturnUnAuthorized() throws Exception {
        Admin admin = TestDataUtil.createAdmin("adminB");
        Admin savedAdmin = adminService.saveAdmin(admin);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/{id}", savedAdmin.getAdminId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    public void testDeleteAdminById_AsSuperAdmin_ShouldReturnNoContent() throws Exception {
        Admin adminToDelete = TestDataUtil.createAdmin("adminB");
        Admin savedAdmin = adminService.saveAdmin(adminToDelete);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/admin/delete/{id}", savedAdmin.getAdminId())
                        .header("Authorization", superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    public void testDeleteAdminById_AsRegularAdmin_ShouldReturnForbidden() throws Exception {
        Admin adminToDelete = TestDataUtil.createAdmin("adminB");
        Admin savedAdmin = adminService.saveAdmin(adminToDelete);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/admin/delete/{id}", savedAdmin.getAdminId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    public void testAdminLogin_ValidCredentials_ShouldReturnOk() throws Exception {
        Admin admin = TestDataUtil.createAdmin("adminB");
        adminService.saveAdmin(admin);

        LoginAdminDTO loginAdminDTO = new LoginAdminDTO();
        loginAdminDTO.setUsername(admin.getUsername());
        loginAdminDTO.setPassword(DEFAULT_PASSWORD);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginAdminDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.tokenDTO").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(admin.getAdminId().toString()));
    }

    @Test
    public void testAdminLogin_TooManyAttempts_ShouldReturnTooManyRequests() throws Exception {
        Admin admin = TestDataUtil.createAdmin("adminB");
        adminService.saveAdmin(admin);

        LoginAdminDTO loginAdminDTO = new LoginAdminDTO();
        loginAdminDTO.setUsername(admin.getUsername());
        loginAdminDTO.setPassword("wrongPassword");

        // Simulate rate limiting by making multiple requests quickly
        for (int i = 0; i < 8; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginAdminDTO)))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        // The next request should be rate limited
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginAdminDTO)))
                .andExpect(MockMvcResultMatchers.status().isTooManyRequests());
    }

    @Test
    public void testRefreshToken_ValidToken_ShouldReturnNewToken() throws Exception {
        // First login to get a refresh token
        Admin admin = TestDataUtil.createAdmin("adminB");
        adminService.saveAdmin(admin);

        LoginAdminDTO loginAdminDTO = new LoginAdminDTO();
        loginAdminDTO.setUsername(admin.getUsername());
        loginAdminDTO.setPassword(DEFAULT_PASSWORD);

        MvcResult loginResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginAdminDTO)))
                .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(response);
        String refreshToken = jsonNode.get("tokenDTO").get("refreshToken").asText();

        // Now test refresh token
        RefreshTokenDTO refreshTokenDTO = new RefreshTokenDTO();
        refreshTokenDTO.setRefreshToken(refreshToken);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshTokenDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.accessToken").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.refreshToken").exists());
    }

    @Test
    public void testRefreshToken_InvalidToken_ShouldReturnBadRequest() throws Exception {
        RefreshTokenDTO refreshTokenDTO = new RefreshTokenDTO();
        refreshTokenDTO.setRefreshToken("invalid.token.here");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshTokenDTO)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void testAdminLogout_ValidRequest_ShouldReturnOk() throws Exception {
        // First login to get tokens
        Admin admin = TestDataUtil.createAdmin("adminB");
        adminService.saveAdmin(admin);

        LoginAdminDTO loginAdminDTO = new LoginAdminDTO();
        loginAdminDTO.setUsername(admin.getUsername());
        loginAdminDTO.setPassword(DEFAULT_PASSWORD);

        MvcResult loginResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginAdminDTO)))
                .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(response);
        String accessToken = jsonNode.get("tokenDTO").get("accessToken").asText();
        String refreshToken = jsonNode.get("tokenDTO").get("refreshToken").asText();

        // Now test logout
        AdminLogoutDTO adminLogoutDTO = new AdminLogoutDTO(admin.getUsername(), refreshToken);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogoutDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testAdminLogout_AlreadyLoggedOut_ShouldReturnConflict() throws Exception {
        Admin admin = TestDataUtil.createAdmin("adminB");
        adminService.saveAdmin(admin);

        LoginAdminDTO loginAdminDTO = new LoginAdminDTO();
        loginAdminDTO.setUsername(admin.getUsername());
        loginAdminDTO.setPassword(DEFAULT_PASSWORD);

        // First login and logout
        String token = obtainAdminToken(admin.getUsername(), DEFAULT_PASSWORD);
        MvcResult loginResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                loginAdminDTO)))
                .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(response);
        String refreshToken = jsonNode.get("tokenDTO").get("refreshToken").asText();

        // First logout (should succeed)
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logout")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AdminLogoutDTO(admin.getUsername(), refreshToken))))
                .andExpect(MockMvcResultMatchers.status().isOk());

        // Second logout with same token (should fail)
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logout")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AdminLogoutDTO(admin.getUsername(), refreshToken))))
                .andExpect(MockMvcResultMatchers.status().isConflict());
    }


    private String obtainAdminToken(String username, String password) throws Exception {
        String loginPayload = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password
        ));

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(response);

        return "Bearer " + jsonNode.get("tokenDTO").get("accessToken").asText();
    }
}
