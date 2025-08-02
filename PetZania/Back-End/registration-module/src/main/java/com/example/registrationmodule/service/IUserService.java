package com.example.registrationmodule.service;

import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;


import java.util.UUID;

@Transactional
public interface IUserService {

    UserProfileDTO registerUser(RegisterUserDTO registerUserDTO);

    void sendVerificationCode(String email);

    void sendDeactivationMessage(String email);

    ProfilePictureDTO getProfilePictureURLByUserId(UUID requesterId, UUID userId);
    Page<UserProfileDTO> getUsers(UUID requesterId, int page, int size, String sortBy, String direction);

    Page<UserProfileDTO> getUsersByPrefixUsername(UUID requesterId, int page, int size, String sortBy, String direction, String prefix);

    void sendResetPasswordOTP(EmailDTO emailDTO);

    void verifyResetOTP(String email, String otp);

    void resetPassword(String email, String otp, String newPassword);
    UserProfileDTO getUserById(UUID requesterId, UUID userId);
  
    void deleteUser(EmailDTO emailDTO);
  
    void sendDeleteConfirmation(User user);

    User saveUser(User user);

    ResponseLoginDTO login(LoginUserDTO loginUserDTO);

    TokenDTO refreshToken(String refreshToken);

    void logout(LogoutDTO logoutDTO);

    void blockUser(BlockUserDTO blockUserDTO);

    void unblockUser(BlockUserDTO blockUserDTO);

    void changePassword(ChangePasswordDTO changePasswordDTO);

    void deleteAll();

    void verifyCode(OTPValidationDTO otpValidationDTO);

    boolean userExistsById(UUID userId);

    UserProfileDTO updateUserById(UUID userId, UpdateUserProfileDto updateUserProfileDto);

}
