package com.example.registrationmodule.controller;

import com.example.registrationmodule.TestDataUtil;
import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.service.IUserService;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import javax.print.attribute.standard.Media;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
public class UserControllerIntegrationTests {

    private MockMvc mockMvc;
    private IUserService userService;
    private ObjectMapper objectMapper;
    private final String DEFAULT_PASSWORD = "Password123#";

    @Autowired
    public UserControllerIntegrationTests(MockMvc mockMvc, IUserService userService, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.userService = userService;
        this.objectMapper = objectMapper;
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
    public void testUpdateUserProfileById_UserNotFound_ShouldReturnNotFound() throws Exception {
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
                .andExpect(MockMvcResultMatchers.status().isNotFound());
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
}