package com.example.registrationmodule.service;

import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;


import java.util.UUID;

@Transactional
public interface IUserService {

    public UserProfileDTO registerUser(RegisterUserDTO registerUserDTO);

    public void sendVerificationCode(String email);

    public void sendDeactivationMessage(String email);

    public ProfilePictureDTO getProfilePictureURLByUserId(UUID userId);
    Page<UserProfileDTO> getUsers(int page, int size, String sortBy, String direction);

    Page<UserProfileDTO> getUsersByPrefixUsername(int page, int size, String sortBy, String direction, String prefix);

    public void sendResetPasswordOTP(EmailDTO emailDTO);

    public void verifyResetOTP(String email, String otp);

    public void resetPassword(String email, String otp, String newPassword);
    UserProfileDTO getUserById(UUID userId);
  
    public void deleteUser(EmailDTO emailDTO);
  
    public void sendDeleteConfirmation(User user);

    User saveUser(User user);

    ResponseLoginDTO login(LoginUserDTO loginUserDTO);

    TokenDTO refreshToken(String refreshToken);

    void logout(LogoutDTO logoutDTO);

    void blockUser(BlockUserDTO blockUserDTO);

    void unblockUser(BlockUserDTO blockUserDTO);

    public void changePassword(ChangePasswordDTO changePasswordDTO);

    void deleteAll();

    void verifyCode(OTPValidationDTO otpValidationDTO);

    boolean userExistsById(UUID userId);

    UserProfileDTO updateUserById(UUID userId, UpdateUserProfileDto updateUserProfileDto);

    UserProfileDTO getUserByUsername(String username);
}
