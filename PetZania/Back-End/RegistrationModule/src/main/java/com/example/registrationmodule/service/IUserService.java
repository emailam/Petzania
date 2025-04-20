package com.example.registrationmodule.service;

import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import jakarta.validation.constraints.Email;

import java.util.List;
import java.util.UUID;

@Transactional
public interface IUserService {
    UserProfileDTO registerUser(RegisterUserDTO registerUserDTO);

    void sendVerificationCode(String email);

    Page<UserProfileDTO> getUsers(int page, int size, String sortBy, String direction);

    UserProfileDTO getUserById(UUID userId);
  
    public void deleteUser(EmailDTO emailDTO);
  
    public void sendDeleteConfirmation(User user);

    User saveUser(User user);

    TokenDTO login(LoginUserDTO loginUserDTO);

    TokenDTO refreshToken(String refreshToken);

    void logout(LogoutDTO logoutDTO);

    void blockUser(BlockUserDTO blockUserDTO);

    void unblockUser(BlockUserDTO blockUserDTO);

    void deleteAll();

    void verifyCode(OTPValidationDTO otpValidationDTO);

    boolean userExistsById(UUID userId);

//    boolean isUserCredentialsValid(String email, String password);

    UserProfileDTO updateUserById(UUID userId, UpdateUserProfileDto updateUserProfileDto);
}
