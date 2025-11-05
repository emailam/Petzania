package com.example.registrationmodule.controller;

import com.example.registrationmodule.TestDataUtil;
import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.service.IAdminService;
import com.example.registrationmodule.service.IUserService;
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

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
public class RateLimitingIntegrationTests {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private IAdminService adminService;
    @Autowired
    private IUserService userService;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    private final int DEFAULT_RATE_LIMIT = 10; // Default @RateLimit requests
    private final int DEFAULT_DURATION = 60;   // Default @RateLimit duration
    private final String DEFAULT_PASSWORD = "Password123#"; // Default password
    private String superAdminToken;
    private String adminToken;
    private String userToken;

    @BeforeEach
    public void setup() throws Exception {
        Admin superAdmin = TestDataUtil.createSuperAdmin("superAdminA");
        adminService.saveAdmin(superAdmin);
        superAdminToken = obtainAdminToken(superAdmin.getUsername(), DEFAULT_PASSWORD);

        Admin admin = TestDataUtil.createAdmin("adminA");
        adminService.saveAdmin(admin);
        adminToken = obtainAdminToken(admin.getUsername(), DEFAULT_PASSWORD);

        User user = TestDataUtil.createTestUser("userA");
        userService.saveUser(user);
        userToken = obtainAccessToken(user.getEmail(), DEFAULT_PASSWORD);
    }
    @AfterEach
    public void cleanup(){
        Set<String> rateLimitKeys = redisTemplate.keys("rate_limit:*");
        if (!rateLimitKeys.isEmpty()) {
            redisTemplate.delete(rateLimitKeys);
        }
    }
    @Test
    public void testRateLimit_GET_Request_ExceedsLimit_ShouldFail() throws Exception {
        String endpoint = "/api/admin/get-all";

        // Make requests up to the limit - should succeed
        for (int i = 0; i < DEFAULT_RATE_LIMIT; i++) {
            mockMvc.perform(MockMvcRequestBuilders.get(endpoint)
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk());
        }

        // This request should be rate limited
        mockMvc.perform(MockMvcRequestBuilders.get(endpoint)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isTooManyRequests());
    }
    @Test
    public void testRateLimit_DifferentEndpoints_ShouldHaveSeparateLimits() throws Exception {
        String endpoint1 = "/api/admin/get-all";
        String endpoint2 = "/api/user/auth/users";

        // Exhaust rate limit on admin endpoint
        for (int i = 0; i < DEFAULT_RATE_LIMIT; i++) {
            mockMvc.perform(MockMvcRequestBuilders.get(endpoint1)
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk());
        }

        // Admin endpoint should be rate limited
        mockMvc.perform(MockMvcRequestBuilders.get(endpoint1)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isTooManyRequests());

        // But user endpoint should still work (different endpoint = separate rate limit)
        mockMvc.perform(MockMvcRequestBuilders.get(endpoint2)
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk()); // or .isNoContent() depending on your endpoint
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
