package com.example.registrationmodule.controller;

import com.example.registrationmodule.TestDataUtil;
import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.model.entity.Block;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.repository.BlockRepository;
import com.example.registrationmodule.service.IAdminService;
import com.example.registrationmodule.service.IUserService;
import com.example.registrationmodule.service.impl.AdminService;
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

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
public class UserControllerIntegrationTests {

    private MockMvc mockMvc;
    private IUserService userService;
    private ObjectMapper objectMapper;
    private IAdminService adminService;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    private final String DEFAULT_PASSWORD = "Password123#";
    private String adminToken;
    private String superAdminToken;

    @Autowired
    private BlockRepository blockRepository;

    @BeforeEach
    public void setupAdminTokens() throws Exception {
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
    public void testLogout_Success() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String token = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);
        String refreshToken = obtainRefreshToken(testUserA.getEmail(), DEFAULT_PASSWORD);
        LogoutDTO logoutDTO = new LogoutDTO(testUserA.getEmail(), refreshToken);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/user/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutDTO))
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("User logged out successfully"));
    }

    @Test
    public void testLogout_UserATriesToLogoutUserB_Failure() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        User testUserB = userService.saveUser(TestDataUtil.createTestUser("userB"));
        String tokenA = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);
        String refreshTokenB = obtainRefreshToken(testUserB.getEmail(), DEFAULT_PASSWORD);
        LogoutDTO logoutDTO = new LogoutDTO(testUserB.getEmail(), refreshTokenB);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/user/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutDTO))
                        .header("Authorization", tokenA))
                .andExpect(MockMvcResultMatchers.status().isBadRequest()); // or isUnauthorized() as per your implementation
    }

    @Test
    public void testLogout_InvalidEmail_Failure() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String tokenA = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);
        String refreshTokenA = obtainRefreshToken(testUserA.getEmail(), DEFAULT_PASSWORD);

        // Malformed email
        LogoutDTO logoutDTO = new LogoutDTO("invalid-email", refreshTokenA);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/user/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutDTO))
                        .header("Authorization", tokenA))
                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());
    }

    @Test
    public void testLogout_WithoutAuthentication_Failure() throws Exception {
        // No authentication token is passed
        LogoutDTO logoutDTO = new LogoutDTO("anyone@email.com", "valid-refresh-token");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/user/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutDTO)))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    public void testGetUsersByPrefixUsername_WithMatchingPrefix_ShouldReturnUsers() throws Exception {
        // Arrange
        User user1 = userService.saveUser(TestDataUtil.createTestUser("testUserA"));
        User user2 = userService.saveUser(TestDataUtil.createTestUser("testUserB"));

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users/test")
                        .header("Authorization", adminToken)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "username")
                        .param("direction", "asc"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.length()").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].username").value(user1.getUsername()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[1].username").value(user2.getUsername()));
    }

    @Test
    public void testGetUsersByPrefixUsername_WithNoMatches_ShouldReturnNoContent() throws Exception {
        // Arrange
        userService.saveUser(TestDataUtil.createTestUser("userA"));
        userService.saveUser(TestDataUtil.createTestUser("userB"));

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users/xyz")
                        .header("Authorization", adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    public void testUserChangesOwnPassword_ShouldReturnSuccess() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String token = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);

        ChangePasswordDTO changePasswordDTO = new ChangePasswordDTO();
        changePasswordDTO.setEmail(testUserA.getEmail());
        changePasswordDTO.setNewPassword("NewSecurePassword123");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDTO))
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("Password changed successfully"));
    }

    @Test
    public void testAdminChangesUserPassword_ShouldReturnSuccess() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));

        ChangePasswordDTO changePasswordDTO = new ChangePasswordDTO();
        changePasswordDTO.setEmail(testUserA.getEmail());
        changePasswordDTO.setNewPassword("NewSecurePassword123");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDTO))
                        .header("Authorization", adminToken))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("Password changed successfully"));
    }

    @Test
    public void testUserDeletesOwnAccount_ShouldReturnSuccess() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String token = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);

        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setEmail(testUserA.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/user/auth/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailDTO))
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("User deleted successfully"));
    }

    @Test
    public void testAdminDeletesUser_ShouldReturnSuccess() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));

        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setEmail(testUserA.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/user/auth/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailDTO))
                        .header("Authorization", adminToken))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("User deleted successfully"));
    }

    @Test
    public void testUserChangeOtherUserPassword_ShouldReturnBadRequest() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        User testUserB = userService.saveUser(TestDataUtil.createTestUser("userB"));
        String tokenA = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);

        ChangePasswordDTO changePasswordDTO = new ChangePasswordDTO();
        changePasswordDTO.setEmail(testUserB.getEmail()); // Targeting another user
        changePasswordDTO.setNewPassword("NewPassword123");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDTO))
                        .header("Authorization", tokenA))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void testGetUserProfilePictureURL_ShouldReturnSuccess() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String token = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);
        String profilePictureURL = testUserA.getProfilePictureURL();

        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/profile-picture-url/{id}", testUserA.getUserId())
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.profilePictureURL").value(profilePictureURL));
    }

    @Test
    public void testGetAnotherUserProfilePictureURL_ShouldSuccess() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        User testUserB = userService.saveUser(TestDataUtil.createTestUser("userB"));
        String token = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);
        String profilePictureURL = testUserB.getProfilePictureURL();

        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/profile-picture-url/{id}", testUserB.getUserId())
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.profilePictureURL").value(profilePictureURL));
    }

    @Test
    public void testGetNonExistentUserProfilePictureURL_ShouldFail() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String token = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);
        String profilePictureURL = "random.com";

        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/profile-picture-url/{id}", UUID.randomUUID())
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testUserDeleteAnotherUser_ShouldReturnBadRequest() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        User testUserB = userService.saveUser(TestDataUtil.createTestUser("userB"));
        String tokenA = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/user/auth/{id}", testUserB.getUserId())
                        .header("Authorization", tokenA))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void testDeleteUserWithoutToken_ShouldReturnUnauthorized() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/user/auth/{id}", testUserA.getUserId()))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    public void testUserDeleteSelf_ShouldReturnOk() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String tokenA = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);

        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setEmail(testUserA.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/user/auth/delete")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testAdminDeletesUser_ShouldReturnOk() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));


        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setEmail(testUserA.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/user/auth/delete")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testSuperAdminDeletesUser_ShouldReturnOk() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));

        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setEmail(testUserA.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/user/auth/delete")
                        .header("Authorization", superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testDeleteNonExistingUser_ShouldReturnNotFound() throws Exception {

        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setEmail("nonexistent@example.com"); // Email does not exist

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/user/auth/delete")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailDTO)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }


    @Test
    public void testChangePasswordWithoutToken_ShouldReturnUnauthorized() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));

        ChangePasswordDTO changePasswordDTO = new ChangePasswordDTO();
        changePasswordDTO.setEmail(testUserA.getEmail());
        changePasswordDTO.setNewPassword("NewPassword123");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDTO)))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    public void testAdminBlockUser_UserNotFound_ShouldReturnNotFound() throws Exception {
        BlockUserDTO blockUserDTO = new BlockUserDTO();
        blockUserDTO.setEmail("user@gmail.com");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/user/auth/block")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockUserDTO)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testAdminDeleteUser_UserNotFound_ShouldReturnNotFound() throws Exception {
        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setEmail("user@gmail.com");

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/user/auth/delete")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailDTO)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testAdminBlockedUser_ShouldPreventLogin() throws Exception {
        User testUser = userService.saveUser(TestDataUtil.createTestUser("userA"));
        testUser.setBlocked(true);
        userService.saveUser(testUser);

        LoginUserDTO loginDTO = new LoginUserDTO();
        loginDTO.setEmail(testUser.getEmail());
        loginDTO.setPassword(DEFAULT_PASSWORD);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void testBlockUser_AsAdmin_ShouldSucceed() throws Exception {
        User testUser = userService.saveUser(TestDataUtil.createTestUser("userA"));
        BlockUserDTO blockUserDTO = new BlockUserDTO();
        blockUserDTO.setEmail(testUser.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/user/auth/block")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockUserDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testBlockUser_AsSuperAdmin_ShouldSucceed() throws Exception {
        User testUser = userService.saveUser(TestDataUtil.createTestUser("userA"));
        BlockUserDTO blockUserDTO = new BlockUserDTO();
        blockUserDTO.setEmail(testUser.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/user/auth/block")
                        .header("Authorization", superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockUserDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testUnblockUser_AsAdmin_ShouldSucceed() throws Exception {
        User testUser = userService.saveUser(TestDataUtil.createTestUser("userA"));
        testUser.setBlocked(true);
        userService.saveUser(testUser);
        BlockUserDTO unblockUserDTO = new BlockUserDTO();
        unblockUserDTO.setEmail(testUser.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/user/auth/unblock")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unblockUserDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testDeleteUser_AsAdmin_ShouldSucceed() throws Exception {
        User testUser = userService.saveUser(TestDataUtil.createTestUser("userA"));
        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setEmail(testUser.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/user/auth/delete")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testDeleteAllUsers_AsSuperAdmin_ShouldSucceed() throws Exception {
        // Create some test users first
        userService.saveUser(TestDataUtil.createTestUser("userA"));
        userService.saveUser(TestDataUtil.createTestUser("userB"));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/user/auth/delete-all")
                        .header("Authorization", superAdminToken))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testDeleteAllUsers_AsRegularAdmin_ShouldSucceed() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/user/auth/delete-all")
                        .header("Authorization", adminToken))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testGetUsers_AsAdmin_ShouldReturnUsers() throws Exception {
        // Create some test users
        userService.saveUser(TestDataUtil.createTestUser("userA"));
        userService.saveUser(TestDataUtil.createTestUser("userB"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users")
                        .header("Authorization", adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.length()").value(2));
    }

    @Test
    public void testBlockNonExistentUser_ShouldReturnNotFound() throws Exception {
        BlockUserDTO blockUserDTO = new BlockUserDTO();
        blockUserDTO.setEmail("userr@gmail.com");
        mockMvc.perform(MockMvcRequestBuilders.post("/api/user/auth/block")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockUserDTO)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testDeleteNonExistentUser_ShouldReturnNotFound() throws Exception {
        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setEmail("userr@gmail.com");

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/user/auth/delete")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailDTO)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Autowired
    public UserControllerIntegrationTests(MockMvc mockMvc, IUserService userService, ObjectMapper objectMapper, AdminService adminService) {
        this.mockMvc = mockMvc;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.adminService = adminService;
    }

    @Test
    public void testVerifyRegistrationOTPCode_Success() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String verificationCode = "123456";
        testUserA.setVerified(false);
        testUserA.setVerificationCode(verificationCode);
        testUserA.setExpirationTime(Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));
        userService.saveUser(testUserA);

        OTPValidationDTO otpValidationDTO = new OTPValidationDTO();
        otpValidationDTO.setOtp(verificationCode);
        otpValidationDTO.setEmail(testUserA.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpValidationDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());

    }

    @Test
    public void testVerifyRegistrationOTPCode_UserAlreadyVerified_Failure() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String verificationCode = "123456";
        testUserA.setVerified(true);
        testUserA.setVerificationCode(verificationCode);
        testUserA.setExpirationTime(Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));
        userService.saveUser(testUserA);

        OTPValidationDTO otpValidationDTO = new OTPValidationDTO();
        otpValidationDTO.setOtp(verificationCode);
        otpValidationDTO.setEmail(testUserA.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpValidationDTO)))
                .andExpect(MockMvcResultMatchers.status().isConflict());

    }

    @Test
    public void testVerifyRegistrationOTPCode_InvalidOTPCode_Failure() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String verificationCode = "123456";
        String invalidCode = "654321";
        testUserA.setVerified(false);
        testUserA.setVerificationCode(verificationCode);
        testUserA.setExpirationTime(Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));
        userService.saveUser(testUserA);

        OTPValidationDTO otpValidationDTO = new OTPValidationDTO();
        otpValidationDTO.setOtp(invalidCode);
        otpValidationDTO.setEmail(testUserA.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpValidationDTO)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

    }

    @Test
    public void testChangePassword_Success() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String newPassword = "abcD1234#";
        String token = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);

        ChangePasswordDTO changePasswordDTO = new ChangePasswordDTO();
        changePasswordDTO.setEmail(testUserA.getEmail());
        changePasswordDTO.setNewPassword(newPassword);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDTO))
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testSendResetOTP_Success() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));

        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setEmail(testUserA.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/send-reset-password-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testSendResetOTP_Failure() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String invalidEmail = "invalidEmail@gmail.com";

        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setEmail(invalidEmail);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/send-reset-password-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailDTO)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testVerifyResetOTP_Success() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        testUserA.setResetCode("123456");
        testUserA.setResetCodeExpirationTime(Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));
        userService.saveUser(testUserA);

        OTPValidationDTO otpValidationDTO = new OTPValidationDTO();
        otpValidationDTO.setEmail(testUserA.getEmail());
        otpValidationDTO.setOtp(testUserA.getResetCode());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/verify-reset-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpValidationDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testVerifyResetOTP_Failure() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String invalidCode = "654321";
        testUserA.setResetCode("123456");
        testUserA.setResetCodeExpirationTime(Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));
        userService.saveUser(testUserA);

        OTPValidationDTO otpValidationDTO = new OTPValidationDTO();
        otpValidationDTO.setEmail(testUserA.getEmail());
        otpValidationDTO.setOtp(invalidCode);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/verify-reset-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpValidationDTO)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void testResetPassword_Success() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String otp = "123456";
        testUserA.setResetCode(otp);
        testUserA.setResetCodeExpirationTime(Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));
        userService.saveUser(testUserA);

        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setPassword("Abcd1234#");
        resetPasswordDTO.setEmail(testUserA.getEmail());
        resetPasswordDTO.setOtp(otp);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testResetPassword_InvalidEmail_Failure() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String otp = "123456";
        String invalidEmail = "invalidEmail@gmail.com";
        testUserA.setResetCode(otp);
        testUserA.setResetCodeExpirationTime(Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));
        userService.saveUser(testUserA);

        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setPassword("Abcd1234#");
        resetPasswordDTO.setEmail(invalidEmail);
        resetPasswordDTO.setOtp(otp);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordDTO)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testResetPassword_InvalidPassword_Failure() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String otp = "123456";
        testUserA.setResetCode(otp);
        testUserA.setResetCodeExpirationTime(Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));
        userService.saveUser(testUserA);

        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setPassword("1234#");
        resetPasswordDTO.setEmail(testUserA.getEmail());
        resetPasswordDTO.setOtp(otp);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordDTO)))
                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());
    }


    @Test
    public void testResetPassword_InvalidOTP_Failure() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String otp = "123456";
        String invalidOTP = "654321";
        testUserA.setResetCode(otp);
        testUserA.setResetCodeExpirationTime(Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));
        userService.saveUser(testUserA);

        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setPassword("Abcd1234#");
        resetPasswordDTO.setEmail(testUserA.getEmail());
        resetPasswordDTO.setOtp(invalidOTP);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordDTO)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }


    @Test
    public void testSignUp_ValidData_ShouldReturnCreated() throws Exception {
        RegisterUserDTO registerUserDTO = new RegisterUserDTO();
        registerUserDTO.setUsername("testName");
        registerUserDTO.setEmail("test@gmail.com");
        registerUserDTO.setPassword("Test123#");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/user/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerUserDTO)))
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    public void testSignUp_InvalidPassword_ShouldReturnBadRequest() throws Exception {
        RegisterUserDTO registerUserDTO = new RegisterUserDTO();
        registerUserDTO.setUsername("testName");
        registerUserDTO.setEmail("test@gmail.com");
        registerUserDTO.setPassword("in1234");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/user/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerUserDTO)))
                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());
    }

    @Test
    public void testSignUp_InvalidMail_ShouldReturnBadRequest() throws Exception {
        RegisterUserDTO registerUserDTO = new RegisterUserDTO();
        registerUserDTO.setUsername("testName");
        registerUserDTO.setEmail("testgmail.com");
        registerUserDTO.setPassword("abc123#A");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/user/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerUserDTO)))
                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());
    }

    @Test
    public void testSignUp_InvalidUserName_ShouldReturnBadRequest() throws Exception {
        RegisterUserDTO registerUserDTO = new RegisterUserDTO();
        registerUserDTO.setUsername("test");
        registerUserDTO.setEmail("test@gmail.com");
        registerUserDTO.setPassword("abc123#A");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/user/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerUserDTO)))
                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());
    }

    @Test
    public void testUserLogin_ValidCredentials_ShouldReturnOk() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));

        LoginUserDTO loginUserDTO = new LoginUserDTO();
        loginUserDTO.setEmail(testUserA.getEmail());
        loginUserDTO.setPassword(DEFAULT_PASSWORD);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginUserDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testUserLogin_InvalidEmail_ShouldReturnUnauthorized() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String invalidEmail = "invalid@gmail.com";

        LoginUserDTO loginUserDTO = new LoginUserDTO();
        loginUserDTO.setEmail(invalidEmail);
        loginUserDTO.setPassword(DEFAULT_PASSWORD);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginUserDTO)))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    public void testUserLogin_InvalidPassword_ShouldReturnUnauthorized() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String wrongPassword = "wassword12#";

        LoginUserDTO loginUserDTO = new LoginUserDTO();
        loginUserDTO.setEmail(testUserA.getEmail());
        loginUserDTO.setPassword(wrongPassword);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginUserDTO)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void testGetUserById_UserExists_ShouldReturnUserProfile() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String token = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);


        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/{userId}", testUserA.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.username").value(testUserA.getUsername()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(testUserA.getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.bio").value(testUserA.getBio()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.profilePictureURL").value(testUserA.getProfilePictureURL()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.phoneNumber").value(testUserA.getPhoneNumber()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.myPets").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.online").value(true));
    }


    @Test
    public void testGetUserById_UserNotFound_ShouldReturnNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String token = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/{userId}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testAdminUpdatesOtherUserProfile_ShouldReturnSuccess() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String token = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);

        UpdateUserProfileDto updateDto = new UpdateUserProfileDto();
        updateDto.setName("Updated Name");
        updateDto.setBio("Updated Bio");
        updateDto.setProfilePictureURL("https://example.com/new-profile.jpg");
        updateDto.setPhoneNumber("123456789");

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/user/auth/{id}", testUserA.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .header("Authorization", adminToken))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(updateDto.getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.bio").value(updateDto.getBio()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.profilePictureURL").value(updateDto.getProfilePictureURL()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.phoneNumber").value(updateDto.getPhoneNumber()));

    }

    @Test
    public void testUpdateUserProfileById_UserExists_ShouldUpdateAndReturnUserProfile() throws Exception {

        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String token = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);

        UpdateUserProfileDto updateDto = new UpdateUserProfileDto();
        updateDto.setName("Updated Name");
        updateDto.setBio("Updated Bio");
        updateDto.setProfilePictureURL("https://example.com/new-profile.jpg");
        updateDto.setPhoneNumber("123456789");

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/user/auth/{id}", testUserA.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(updateDto.getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.bio").value(updateDto.getBio()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.profilePictureURL").value(updateDto.getProfilePictureURL()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.phoneNumber").value(updateDto.getPhoneNumber()));
    }

    @Test
    public void testUpdateAnotherUserProfileById_ShouldReturnBadRequest() throws Exception {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        UpdateUserProfileDto updateDto = new UpdateUserProfileDto();
        updateDto.setName("Updated Name");

        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String token = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/user/auth/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void testUpdateUserProfileById_PartialUpdate_ShouldReturnUpdatedProfile() throws Exception {
        // Arrange
        UpdateUserProfileDto updateDto = new UpdateUserProfileDto();
        updateDto.setBio("Updated Bio Only");

        User testUserA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String token = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/user/auth/{id}", testUserA.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.bio").value(updateDto.getBio()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(testUserA.getName()));
    }

    @Test
    public void testGetUsers_WhenUserABlocksUserB_ShouldNotReturnUserB() throws Exception {
        // Arrange
        User userA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        User userB = userService.saveUser(TestDataUtil.createTestUser("userB"));

        // Get initial count (includes dummy users)
        String tokenA = obtainAccessToken(userA.getEmail(), DEFAULT_PASSWORD);
        MvcResult initialResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users")
                        .header("Authorization", tokenA)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        JsonNode initialResponse = objectMapper.readTree(initialResult.getResponse().getContentAsString());
        int initialCount = initialResponse.get("content").size();

        // Create block relationship: userA blocks userB
        Block block = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userA)
                .blocked(userB)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(block);

        // Act & Assert - Should have one less user (userB should be excluded)
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users")
                        .header("Authorization", tokenA)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.length()").value(initialCount - 1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userB.getUserId() + "')]").doesNotExist());
    }

    @Test
    public void testGetUsers_WhenUserBBlocksUserA_ShouldNotReturnUserB() throws Exception {
        // Arrange
        User userA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        User userB = userService.saveUser(TestDataUtil.createTestUser("userB"));

        // Create block relationship: userB blocks userA
        Block block = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userB)
                .blocked(userA)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(block);

        String tokenA = obtainAccessToken(userA.getEmail(), DEFAULT_PASSWORD);

        // Act & Assert - UserA should not see UserB in the list (bidirectional blocking)
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users")
                        .header("Authorization", tokenA)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userB.getUserId() + "')]").doesNotExist());
    }

    @Test
    public void testGetUsers_WithMutualBlocking_ShouldExcludeBothUsers() throws Exception {
        // Arrange
        User userA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        User userB = userService.saveUser(TestDataUtil.createTestUser("userB"));

        // Create mutual block relationships
        Block blockAB = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userA)
                .blocked(userB)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(blockAB);

        Block blockBA = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userB)
                .blocked(userA)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(blockBA);

        String tokenA = obtainAccessToken(userA.getEmail(), DEFAULT_PASSWORD);

        // Act & Assert - UserA should not see UserB
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users")
                        .header("Authorization", tokenA)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userB.getUserId() + "')]").doesNotExist());
    }

    @Test
    public void testGetUsers_AsAdmin_ShouldReturnAllUsersRegardlessOfBlocking() throws Exception {
        // Arrange
        User userA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        User userB = userService.saveUser(TestDataUtil.createTestUser("userB"));

        // Get initial count with admin token
        MvcResult initialResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users")
                        .header("Authorization", adminToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        JsonNode initialResponse = objectMapper.readTree(initialResult.getResponse().getContentAsString());
        int initialCount = initialResponse.get("content").size();

        // Create block relationship
        Block block = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userA)
                .blocked(userB)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(block);

        // Act & Assert - Admin should still see the same count (blocking doesn't affect admin)
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users")
                        .header("Authorization", adminToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.length()").value(initialCount))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userA.getUserId() + "')]").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userB.getUserId() + "')]").exists());
    }

    @Test
    public void testGetUsers_WithMultipleBlockedUsers_ShouldExcludeAllBlockedUsers() throws Exception {
        // Arrange
        User userA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        User userB = userService.saveUser(TestDataUtil.createTestUser("userB"));
        User userC = userService.saveUser(TestDataUtil.createTestUser("userC"));

        // UserA blocks UserB and UserC
        Block blockAB = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userA)
                .blocked(userB)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(blockAB);

        Block blockAC = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userA)
                .blocked(userC)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(blockAC);

        String tokenA = obtainAccessToken(userA.getEmail(), DEFAULT_PASSWORD);

        // Act & Assert - Should not return UserB or UserC
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users")
                        .header("Authorization", tokenA)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userB.getUserId() + "')]").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userC.getUserId() + "')]").doesNotExist());
    }

    @Test
    public void testGetUsersByPrefixUsername_WhenUserABlocksUserB_ShouldNotReturnUserB() throws Exception {
        // Arrange
        User userA = userService.saveUser(TestDataUtil.createTestUser("testUserA")); // username: testUserA
        User userB = userService.saveUser(TestDataUtil.createTestUser("testUserB")); // username: testUserB
        User userC = userService.saveUser(TestDataUtil.createTestUser("testUserC")); // username: testUserC

        // Create block relationship: userA blocks userB
        Block block = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userA)
                .blocked(userB)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(block);

        String tokenA = obtainAccessToken(userA.getEmail(), DEFAULT_PASSWORD);

        // Act & Assert - Should not return userB when searching for "test" prefix
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users/test")
                        .header("Authorization", tokenA)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userB.getUserId() + "')]").doesNotExist());
    }

    @Test
    public void testGetUsersByPrefixUsername_WhenUserBBlocksUserA_ShouldNotReturnUserB() throws Exception {
        // Arrange
        User userA = userService.saveUser(TestDataUtil.createTestUser("testUserA"));
        User userB = userService.saveUser(TestDataUtil.createTestUser("testUserB"));
        User userC = userService.saveUser(TestDataUtil.createTestUser("testUserC"));

        // Create block relationship: userB blocks userA
        Block block = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userB)
                .blocked(userA)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(block);

        String tokenA = obtainAccessToken(userA.getEmail(), DEFAULT_PASSWORD);

        // Act & Assert - Should not return userB
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users/test")
                        .header("Authorization", tokenA)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userB.getUserId() + "')]").doesNotExist());
    }

    @Test
    public void testGetUsersByPrefixUsername_WithBlockingAndMultipleMatches_ShouldReturnOnlyUnblockedUsers() throws Exception {
        // Arrange
        User userA = userService.saveUser(TestDataUtil.createTestUser("testUserA")); // testUser
        User userB = userService.saveUser(TestDataUtil.createTestUser("testUserB")); // testUserB
        User userC = userService.saveUser(TestDataUtil.createTestUser("testUserC")); // testUserC

        // UserA blocks UserB
        Block block = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userA)
                .blocked(userB)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(block);

        String tokenA = obtainAccessToken(userA.getEmail(), DEFAULT_PASSWORD);

        // Act & Assert - Should return userC but not userB
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users/test")
                        .header("Authorization", tokenA)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userB.getUserId() + "')]").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userC.getUserId() + "')]").exists())
                .andReturn();
    }

    @Test
    public void testGetUsersByPrefixUsername_AsAdmin_ShouldReturnAllMatchingUsersRegardlessOfBlocking() throws Exception {
        // Arrange
        User userA = userService.saveUser(TestDataUtil.createTestUser("testUserA"));
        User userB = userService.saveUser(TestDataUtil.createTestUser("testUserB"));

        // Create block relationship
        Block block = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userA)
                .blocked(userB)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(block);

        // Act & Assert - Admin should see both users
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users/test")
                        .header("Authorization", adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userA.getUserId() + "')]").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userB.getUserId() + "')]").exists());
    }

    @Test
    public void testGetUserById_WhenUserABlocksUserB_ShouldReturnForbidden() throws Exception {
        // Arrange
        User userA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        User userB = userService.saveUser(TestDataUtil.createTestUser("userB"));

        // Create block relationship: userA blocks userB
        Block block = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userA)
                .blocked(userB)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(block);

        String tokenA = obtainAccessToken(userA.getEmail(), DEFAULT_PASSWORD);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/{id}", userB.getUserId())
                        .header("Authorization", tokenA))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Cannot Perform This Operation Due To Existence Of Blocking"));
    }

    @Test
    public void testGetUserById_WhenUserBBlocksUserA_ShouldReturnForbidden() throws Exception {
        // Arrange
        User userA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        User userB = userService.saveUser(TestDataUtil.createTestUser("userB"));

        // Create block relationship: userB blocks userA
        Block block = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userB)
                .blocked(userA)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(block);

        String tokenA = obtainAccessToken(userA.getEmail(), DEFAULT_PASSWORD);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/{id}", userB.getUserId())
                        .header("Authorization", tokenA))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Cannot Perform This Operation Due To Existence Of Blocking"));
    }

    @Test
    public void testGetUsers_WithChainedBlocking_ShouldHandleComplexBlockingChains() throws Exception {
        // Arrange - Create a chain: A blocks B, B blocks C, C blocks D
        User userA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        User userB = userService.saveUser(TestDataUtil.createTestUser("userB"));
        User userC = userService.saveUser(TestDataUtil.createTestUser("userC"));
        User userD = userService.saveUser(TestDataUtil.createTestUser("userD"));
        User userE = userService.saveUser(TestDataUtil.createTestUser("userE"));

        // Create blocking chain
        Block blockAB = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userA)
                .blocked(userB)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(blockAB);

        Block blockBC = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userB)
                .blocked(userC)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(blockBC);

        Block blockCD = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userC)
                .blocked(userD)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(blockCD);

        String tokenA = obtainAccessToken(userA.getEmail(), DEFAULT_PASSWORD);
        String tokenC = obtainAccessToken(userC.getEmail(), DEFAULT_PASSWORD);

        // Act & Assert - UserA should not see UserB, UserC should not see UserB or UserD
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users")
                        .header("Authorization", tokenA)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userB.getUserId() + "')]").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userC.getUserId() + "')]").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userD.getUserId() + "')]").exists());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users")
                        .header("Authorization", tokenC)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userB.getUserId() + "')]").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userD.getUserId() + "')]").doesNotExist());
    }

    @Test
    public void testGetUsers_WithCircularBlocking_ShouldHandleCircularReferences() throws Exception {
        // Arrange - Create circular blocking: A blocks B, B blocks C, C blocks A
        User userA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        User userB = userService.saveUser(TestDataUtil.createTestUser("userB"));
        User userC = userService.saveUser(TestDataUtil.createTestUser("userC"));
        User userD = userService.saveUser(TestDataUtil.createTestUser("userD"));

        // Create circular blocking
        Block blockAB = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userA)
                .blocked(userB)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(blockAB);

        Block blockBC = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userB)
                .blocked(userC)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(blockBC);

        Block blockCA = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userC)
                .blocked(userA)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(blockCA);

        String tokenA = obtainAccessToken(userA.getEmail(), DEFAULT_PASSWORD);
        String tokenB = obtainAccessToken(userB.getEmail(), DEFAULT_PASSWORD);
        String tokenC = obtainAccessToken(userC.getEmail(), DEFAULT_PASSWORD);

        // Act & Assert - Each user should only see userD
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users")
                        .header("Authorization", tokenA)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userB.getUserId() + "')]").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userC.getUserId() + "')]").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[?(@.userId == '" + userD.getUserId() + "')]").exists());
    }

    @Test
    public void testGetUserById_WithSelfReference_ShouldReturnOwnProfile() throws Exception {
        // Arrange
        User userA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        String tokenA = obtainAccessToken(userA.getEmail(), DEFAULT_PASSWORD);

        // Act & Assert - User should be able to see their own profile
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/{id}", userA.getUserId())
                        .header("Authorization", tokenA))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(userA.getUserId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.username").value(userA.getUsername()));
    }

    @Test
    public void testGetUserById_WithBlockingAndUnblocking_ShouldReflectCurrentBlockStatus() throws Exception {
        // Arrange
        User userA = userService.saveUser(TestDataUtil.createTestUser("userA"));
        User userB = userService.saveUser(TestDataUtil.createTestUser("userB"));
        String tokenA = obtainAccessToken(userA.getEmail(), DEFAULT_PASSWORD);

        // Initially no blocking - should work
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/{id}", userB.getUserId())
                        .header("Authorization", tokenA))
                .andExpect(MockMvcResultMatchers.status().isOk());

        // Create block
        Block block = Block.builder()
                .blockId(UUID.randomUUID())
                .blocker(userA)
                .blocked(userB)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        blockRepository.save(block);

        // Now should be blocked
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/{id}", userB.getUserId())
                        .header("Authorization", tokenA))
                .andExpect(MockMvcResultMatchers.status().isForbidden());

        // Remove block
        blockRepository.delete(block);

        // Should work again
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/{id}", userB.getUserId())
                        .header("Authorization", tokenA))
                .andExpect(MockMvcResultMatchers.status().isOk());
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

    private String obtainRefreshToken(String email, String password) throws Exception {
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
        return jsonNode.get("tokenDTO").get("refreshToken").asText();
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