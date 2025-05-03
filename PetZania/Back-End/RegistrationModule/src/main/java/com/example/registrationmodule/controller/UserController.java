package com.example.registrationmodule.controller;

import com.example.registrationmodule.exception.user.UserNotFound;
import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.Media;
import com.example.registrationmodule.service.ICloudService;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.model.entity.UserPrincipal;
import com.example.registrationmodule.service.IUserService;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/user/auth")
@RequiredArgsConstructor
public class UserController {
    private final IUserService userService;
    private final ICloudService cloudService;


    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody @Valid LogoutDTO logoutDTO) {
        userService.logout(logoutDTO);
        return ResponseEntity.ok("User logged out successfully");
    }

    @PostMapping("/resendOTP")
    public ResponseEntity<String> resendOTP(@RequestBody @Valid EmailDTO emailDTO) {
        userService.sendVerificationCode(emailDTO.getEmail());
        return ResponseEntity.ok("A new OTP was sent");
    }

    @PostMapping("/block")
    public ResponseEntity<String> blockUser(@RequestBody @Valid BlockUserDTO blockUserDTO) {
        userService.blockUser(blockUserDTO);
        return ResponseEntity.ok("User is blocked successfully");
    }

    @PostMapping("/unblock")
    public ResponseEntity<String> unblockUser(@RequestBody @Valid BlockUserDTO blockUserDTO) {
        userService.unblockUser(blockUserDTO);
        return ResponseEntity.ok("User is unblocked successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseLoginDTO> login(@RequestBody @Valid LoginUserDTO loginUserDTO) {
        ResponseLoginDTO token = userService.login(loginUserDTO);
        return ResponseEntity.status(HttpStatus.OK).body(token);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenDTO> refreshToken(@RequestBody RefreshTokenDTO refreshTokenDTO) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.refreshToken(refreshTokenDTO.getRefreshToken()));
    }

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponseDTO> signup(@RequestBody @Valid RegisterUserDTO registerUserDTO) {
        UserProfileDTO userProfileDTO = userService.registerUser(registerUserDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SignUpResponseDTO("User registered successfully", userProfileDTO));
    }

    @PatchMapping("/{id}/files")
    public ResponseEntity<UserProfileDTO> updateUserFiles(@PathVariable(name = "id") UUID userId,
                                                               @RequestPart(value = "profile picture", required = false) MultipartFile profilePicture) throws IOException {
        if (!userService.userExistsById(userId)) {
            throw new UserNotFound("User not found with ID: " + userId);
        }

        UpdateUserProfileDto updateUserProfileDto = new UpdateUserProfileDto();

        if(profilePicture != null) {
            if(!profilePicture.isEmpty()) {
                Media media = cloudService.uploadAndSaveMedia(profilePicture, true);
                String cdnUrl = cloudService.getMediaUrl(media.getMediaId());
                updateUserProfileDto.setProfilePictureURL(cdnUrl);
            }
            else {
                updateUserProfileDto.setProfilePictureURL("");
            }
        }

        UserProfileDTO userProfileDto = userService.updateUserById(userId, updateUserProfileDto);
        return ResponseEntity.ok(userProfileDto);
    }


    @PatchMapping("/{id}")
    public ResponseEntity<UserProfileDTO> updateUserProfile(@PathVariable("id") UUID userId,
                                                            @RequestBody UpdateUserProfileDto updateUserProfileDto) throws AccessDeniedException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal userPrincipal) {
            System.out.println(((UserPrincipal) principal).getUserId());
            System.out.println(userId);
            if (!userPrincipal.getUserId().equals(userId)) {
                throw new AccessDeniedException("You can only update your own profile");
            }
        }

        UserProfileDTO userProfileDTO = userService.updateUserById(userId, updateUserProfileDto);
        return ResponseEntity.ok(userProfileDTO);
    }

    @PutMapping("/verify")
    public ResponseEntity<String> verifyCode(@RequestBody @Valid OTPValidationDTO otpValidationDTO) {
        userService.verifyCode(otpValidationDTO);
        return ResponseEntity.ok("OTP verification successful");
    }

    @PutMapping("/changePassword")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordDTO changePasswordDTO) throws AccessDeniedException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal userPrincipal) {
            if (!userPrincipal.getEmail().equals(changePasswordDTO.getEmail())) {
                throw new AccessDeniedException("You can only change your own password");
            }
        }

        userService.changePassword(changePasswordDTO);
        return ResponseEntity.ok("Password changed successfully");
    }

    @PutMapping("/sendResetPasswordOTP")
    public ResponseEntity<String> sendResetOTP(@RequestBody EmailDTO emailDTO) {
        userService.sendResetPasswordOTP(emailDTO);
        return ResponseEntity.ok("OTP sent successfully");
    }

    @PutMapping("/verifyResetOTP")
    public ResponseEntity<String> verifyResetOTP(@RequestBody OTPValidationDTO otpValidationDTO) {
        userService.verifyResetOTP(otpValidationDTO.getEmail(), otpValidationDTO.getOtp());
        return ResponseEntity.ok("OTP verification successful");
    }

    @PutMapping("/resetPassword")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordDTO resetPasswordDTO) {
        userService.resetPassword(resetPasswordDTO.getEmail(), resetPasswordDTO.getOtp(), resetPasswordDTO.getPassword());
        return ResponseEntity.ok("Password changed successfully");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteUser(@Valid @RequestBody EmailDTO emailDTO) throws AccessDeniedException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        if(principal instanceof UserPrincipal userPrincipal){
            if(!userPrincipal.getEmail().equals(emailDTO.getEmail())){
                throw new AccessDeniedException("You can delete your own account");
            }
        }

        userService.deleteUser(emailDTO);
        return ResponseEntity.ok("User deleted successfully");
    }

    @DeleteMapping("/deleteAll")
    public ResponseEntity<String> deleteAllUsers() {
        userService.deleteAll();
        return ResponseEntity.ok("All users deleted successfully");
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserProfileDTO>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "userId") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Page<UserProfileDTO> users = userService.getUsers(page, size, sortBy, direction);
        return users.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileDTO> getUserById(@PathVariable("id") UUID userId) {
        UserProfileDTO user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserProfileDTO> getUserByUsername(@PathVariable("username") String username) {
        UserProfileDTO user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }
}
