package com.example.registrationmodule.controller;

import com.example.registrationmodule.TestDataUtil;
import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.service.IAdminService;
import com.example.registrationmodule.service.IUserService;
import com.example.registrationmodule.service.impl.AdminService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
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
    private final String DEFAULT_PASSWORD = "Password123#";
    private String adminToken;
    private String superAdminToken;

    @BeforeEach
    public void setupAdminTokens() throws Exception {
        Admin superAdmin = TestDataUtil.createSuperAdminA();
        adminService.saveAdmin(superAdmin);
        superAdminToken = obtainAdminToken(superAdmin.getUsername(), DEFAULT_PASSWORD);

        Admin admin = TestDataUtil.createAdminA();
        adminService.saveAdmin(admin);
        adminToken = obtainAdminToken(admin.getUsername(), DEFAULT_PASSWORD);
    }

    @Test
    public void testGetUsersByPrefixUsername_WithMatchingPrefix_ShouldReturnUsers() throws Exception {
        // Arrange
        User user1 = userService.saveUser(TestDataUtil.createTestUserA());
        User user2 = userService.saveUser(TestDataUtil.createTestUserB());

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users/test")
                        .header("Authorization", adminToken)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "username")
                        .param("direction", "asc"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.length()").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].username").value("testUser"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[1].username").value("testUserB"));
    }

    @Test
    public void testGetUsersByPrefixUsername_WithNoMatches_ShouldReturnNoContent() throws Exception {
        // Arrange
        userService.saveUser(TestDataUtil.createTestUserA());
        userService.saveUser(TestDataUtil.createTestUserB());

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users/xyz")
                        .header("Authorization", adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    public void testUserChangesOwnPassword_ShouldReturnSuccess() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
        String token = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);

        ChangePasswordDTO changePasswordDTO = new ChangePasswordDTO();
        changePasswordDTO.setEmail(testUserA.getEmail());
        changePasswordDTO.setNewPassword("NewSecurePassword123");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/changePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDTO))
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("Password changed successfully"));
    }

    @Test
    public void testAdminChangesUserPassword_ShouldReturnSuccess() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());

        ChangePasswordDTO changePasswordDTO = new ChangePasswordDTO();
        changePasswordDTO.setEmail(testUserA.getEmail());
        changePasswordDTO.setNewPassword("NewSecurePassword123");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/changePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDTO))
                        .header("Authorization", adminToken))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("Password changed successfully"));
    }

    @Test
    public void testUserDeletesOwnAccount_ShouldReturnSuccess() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
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
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());

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
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
        User testUserB = userService.saveUser(TestDataUtil.createTestUserB());
        String tokenA = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);

        ChangePasswordDTO changePasswordDTO = new ChangePasswordDTO();
        changePasswordDTO.setEmail(testUserB.getEmail()); // Targeting another user
        changePasswordDTO.setNewPassword("NewPassword123");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/changePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDTO))
                        .header("Authorization", tokenA))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
    @Test
    public void testUserDeleteAnotherUser_ShouldReturnBadRequest() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
        User testUserB = userService.saveUser(TestDataUtil.createTestUserB());
        String tokenA = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/user/auth/{id}", testUserB.getUserId())
                        .header("Authorization", tokenA))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
    @Test
    public void testDeleteUserWithoutToken_ShouldReturnUnauthorized() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/user/auth/{id}", testUserA.getUserId()))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }
    @Test
    public void testUserDeleteSelf_ShouldReturnOk() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
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
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());


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
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());

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
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());

        ChangePasswordDTO changePasswordDTO = new ChangePasswordDTO();
        changePasswordDTO.setEmail(testUserA.getEmail());
        changePasswordDTO.setNewPassword("NewPassword123");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/changePassword")
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
        User testUser = userService.saveUser(TestDataUtil.createTestUserA());
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
        User testUser = userService.saveUser(TestDataUtil.createTestUserA());
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
        User testUser = userService.saveUser(TestDataUtil.createTestUserA());
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
        User testUser = userService.saveUser(TestDataUtil.createTestUserA());
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
        User testUser = userService.saveUser(TestDataUtil.createTestUserA());
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
        userService.saveUser(TestDataUtil.createTestUserA());
        userService.saveUser(TestDataUtil.createTestUserB());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/user/auth/deleteAll")
                        .header("Authorization", superAdminToken))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testDeleteAllUsers_AsRegularAdmin_ShouldSucceed() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/user/auth/deleteAll")
                        .header("Authorization", adminToken))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testGetUsers_AsAdmin_ShouldReturnUsers() throws Exception {
        // Create some test users
        userService.saveUser(TestDataUtil.createTestUserA());
        userService.saveUser(TestDataUtil.createTestUserB());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/users")
                        .header("Authorization", adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.length()").value(7));
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
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
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
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
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
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
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
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
        String newPassword = "abcD1234#";
        String token = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);

        ChangePasswordDTO changePasswordDTO = new ChangePasswordDTO();
        changePasswordDTO.setEmail(testUserA.getEmail());
        changePasswordDTO.setNewPassword(newPassword);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/changePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDTO))
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testSendResetOTP_Success() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());

        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setEmail(testUserA.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/sendResetPasswordOTP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testSendResetOTP_Failure() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
        String invalidEmail = "invalidEmail@gmail.com";

        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setEmail(invalidEmail);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/sendResetPasswordOTP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailDTO)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testVerifyResetOTP_Success() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
        testUserA.setResetCode("123456");
        testUserA.setResetCodeExpirationTime(Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));
        userService.saveUser(testUserA);

        OTPValidationDTO otpValidationDTO = new OTPValidationDTO();
        otpValidationDTO.setEmail(testUserA.getEmail());
        otpValidationDTO.setOtp(testUserA.getResetCode());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/verifyResetOTP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpValidationDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testVerifyResetOTP_Failure() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
        String invalidCode = "654321";
        testUserA.setResetCode("123456");
        testUserA.setResetCodeExpirationTime(Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));
        userService.saveUser(testUserA);

        OTPValidationDTO otpValidationDTO = new OTPValidationDTO();
        otpValidationDTO.setEmail(testUserA.getEmail());
        otpValidationDTO.setOtp(invalidCode);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/verifyResetOTP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpValidationDTO)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void testResetPassword_Success() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
        String otp = "123456";
        testUserA.setResetCode(otp);
        testUserA.setResetCodeExpirationTime(Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));
        userService.saveUser(testUserA);

        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setPassword("Abcd1234#");
        resetPasswordDTO.setEmail(testUserA.getEmail());
        resetPasswordDTO.setOtp(otp);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/resetPassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testResetPassword_InvalidEmail_Failure() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
        String otp = "123456";
        String invalidEmail = "invalidEmail@gmail.com";
        testUserA.setResetCode(otp);
        testUserA.setResetCodeExpirationTime(Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));
        userService.saveUser(testUserA);

        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setPassword("Abcd1234#");
        resetPasswordDTO.setEmail(invalidEmail);
        resetPasswordDTO.setOtp(otp);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/resetPassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordDTO)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testResetPassword_InvalidPassword_Failure() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
        String otp = "123456";
        testUserA.setResetCode(otp);
        testUserA.setResetCodeExpirationTime(Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));
        userService.saveUser(testUserA);

        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setPassword("1234#");
        resetPasswordDTO.setEmail(testUserA.getEmail());
        resetPasswordDTO.setOtp(otp);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/resetPassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordDTO)))
                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());
    }


    @Test
    public void testResetPassword_InvalidOTP_Failure() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
        String otp = "123456";
        String invalidOTP = "654321";
        testUserA.setResetCode(otp);
        testUserA.setResetCodeExpirationTime(Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));
        userService.saveUser(testUserA);

        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setPassword("Abcd1234#");
        resetPasswordDTO.setEmail(testUserA.getEmail());
        resetPasswordDTO.setOtp(invalidOTP);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/auth/resetPassword")
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
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());

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
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
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
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
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
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
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

        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
        String token = obtainAccessToken(testUserA.getEmail(), DEFAULT_PASSWORD);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/auth/{userId}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testAdminUpdatesOtherUserProfile_ShouldReturnSuccess() throws Exception {
        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
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

        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
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

        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
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

        User testUserA = userService.saveUser(TestDataUtil.createTestUserA());
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