package com.example.registrationmodule.service;

import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.User;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Email;

import java.util.List;
import java.util.UUID;

@Transactional
public interface IUserService {
    public UserProfileDTO registerUser(RegisterUserDTO registerUserDTO);

    public void sendVerificationCode(String email);

    public List<UserProfileDTO> getUsers();


    public UserProfileDTO getUserById(UUID userId);

    public void deleteUser(EmailDTO emailDTO);

    public void sendDeleteConfirmation(User user);

    public User saveUser(User user);

    public TokenDTO login(LoginUserDTO loginUserDTO);


    public TokenDTO refreshToken(String refreshToken);

    public void logout(LogoutDTO logoutDTO);

    public void blockUser(BlockUserDTO blockUserDTO);

    public void unblockUser(BlockUserDTO blockUserDTO);

    void deleteAll();

    void verifyCode(OTPValidationDTO otpValidationDTO);

    boolean userExistsById(UUID userId);


//    boolean isUserCredentialsValid(String email, String password);

    UserProfileDTO updateUserById(UUID userId, UpdateUserProfileDto updateUserProfileDto);
}
