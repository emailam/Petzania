package com.example.registrationmodule.controller;

import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.AdminPrincipal;
import com.example.registrationmodule.model.entity.UserPrincipal;
import com.example.registrationmodule.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.nio.file.AccessDeniedException;
import java.util.UUID;

@RestController
@RequestMapping("/api/user/auth")
@RequiredArgsConstructor
@Tag(name = "User Authentication", description = "Operations related to user authentication and account management")
public class UserController {
    private final IUserService userService;

    @Operation(summary = "Logout user")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody @Valid LogoutDTO logoutDTO) {
        userService.logout(logoutDTO);
        return ResponseEntity.ok("User logged out successfully");
    }

    @Operation(summary = "Resend verification OTP")
    @PostMapping("/resend-otp")
    public ResponseEntity<String> resendOTP(@RequestBody @Valid EmailDTO emailDTO) {
        userService.sendVerificationCode(emailDTO.getEmail());
        return ResponseEntity.ok("A new OTP was sent");
    }

    @Operation(summary = "Block a user")
    @PostMapping("/block")
    public ResponseEntity<String> blockUser(@RequestBody @Valid BlockUserDTO blockUserDTO) {
        userService.blockUser(blockUserDTO);
        return ResponseEntity.ok("User is blocked successfully");
    }

    @Operation(summary = "Unblock a user")
    @PostMapping("/unblock")
    public ResponseEntity<String> unblockUser(@RequestBody @Valid BlockUserDTO blockUserDTO) {
        userService.unblockUser(blockUserDTO);
        return ResponseEntity.ok("User is unblocked successfully");
    }

    @Operation(summary = "Login user")
    @PostMapping("/login")
    public ResponseEntity<ResponseLoginDTO> login(@RequestBody @Valid LoginUserDTO loginUserDTO) {
        ResponseLoginDTO token = userService.login(loginUserDTO);
        return ResponseEntity.status(HttpStatus.OK).body(token);
    }


    @Operation(summary = "Refresh authentication token")
    @PostMapping("/refresh-token")
    public ResponseEntity<TokenDTO> refreshToken(@RequestBody RefreshTokenDTO refreshTokenDTO) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.refreshToken(refreshTokenDTO.getRefreshToken()));
    }

    @Operation(summary = "Register new user")
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponseDTO> signup(@RequestBody @Valid RegisterUserDTO registerUserDTO) {
        UserProfileDTO userProfileDTO = userService.registerUser(registerUserDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SignUpResponseDTO("User registered successfully", userProfileDTO));
    }

    @Operation(summary = "Update current user profile")
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

    @Operation(summary = "Verify OTP code")
    @PutMapping("/verify")
    public ResponseEntity<String> verifyCode(@RequestBody @Valid OTPValidationDTO otpValidationDTO) {
        userService.verifyCode(otpValidationDTO);
        return ResponseEntity.ok("OTP verification successful");
    }

    @Operation(summary = "Change user password")
    @PutMapping("/change-password")
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

    @Operation(summary = "Send OTP for password reset")
    @PutMapping("/send-reset-password-otp")
    public ResponseEntity<String> sendResetOTP(@RequestBody EmailDTO emailDTO) {
        userService.sendResetPasswordOTP(emailDTO);
        return ResponseEntity.ok("OTP sent successfully");
    }

    @Operation(summary = "Verify OTP for password reset")
    @PutMapping("/verify-reset-otp")
    public ResponseEntity<String> verifyResetOTP(@RequestBody OTPValidationDTO otpValidationDTO) {
        userService.verifyResetOTP(otpValidationDTO.getEmail(), otpValidationDTO.getOtp());
        return ResponseEntity.ok("OTP verification successful");
    }

    @Operation(summary = "Reset password using OTP")
    @PutMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordDTO resetPasswordDTO) {
        userService.resetPassword(resetPasswordDTO.getEmail(), resetPasswordDTO.getOtp(), resetPasswordDTO.getPassword());
        return ResponseEntity.ok("Password changed successfully");
    }

    @Operation(summary = "Delete user account")
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteUser(@Valid @RequestBody EmailDTO emailDTO) throws AccessDeniedException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal userPrincipal) {
            if (!userPrincipal.getEmail().equals(emailDTO.getEmail())) {
                throw new AccessDeniedException("You can delete your own account");
            }
        }

        userService.deleteUser(emailDTO);
        return ResponseEntity.ok("User deleted successfully");
    }

    @Operation(summary = "Delete all users")
    @DeleteMapping("/delete-all")
    public ResponseEntity<String> deleteAllUsers() {
        userService.deleteAll();
        return ResponseEntity.ok("All users deleted successfully");
    }

    @Operation(summary = "Get paginated list of users")
    @GetMapping("/users")
    public ResponseEntity<Page<UserProfileDTO>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "userId") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        UUID requesterId = UUID.randomUUID();
        if (principal instanceof UserPrincipal userPrincipal) {
            requesterId = userPrincipal.getUserId();
        }
        Page<UserProfileDTO> users = userService.getUsers(requesterId, page, size, sortBy, direction);
        return users.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(users);
    }

    @Operation(summary = "Get users by username prefix")
    @GetMapping("/users/{prefix}")
    public ResponseEntity<Page<UserProfileDTO>> getUsersByPrefixUsername(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @PathVariable("prefix") String prefix
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        UUID requesterId = UUID.randomUUID();
        if (principal instanceof UserPrincipal userPrincipal) {
            requesterId = userPrincipal.getUserId();
        }
        Page<UserProfileDTO> users = userService.getUsersByPrefixUsername(requesterId, page, size, sortBy, direction, prefix);
        return users.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(users);
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileDTO> getUserById(@PathVariable("id") UUID userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        UUID requesterId = UUID.randomUUID();
        if (principal instanceof UserPrincipal userPrincipal) {
            requesterId = userPrincipal.getUserId();
        }
        UserProfileDTO user = userService.getUserById(requesterId, userId);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Get Profile Picture By UserID")
    @GetMapping("/profile-picture-url/{id}")
    public ResponseEntity<ProfilePictureDTO> getProfilePictureURLByUserId(@PathVariable("id") UUID userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        UUID requesterId = UUID.randomUUID();
        if (principal instanceof UserPrincipal userPrincipal) {
            requesterId = userPrincipal.getUserId();
        }
        ProfilePictureDTO profilePictureDTO = userService.getProfilePictureURLByUserId(requesterId, userId);
        return ResponseEntity.ok(profilePictureDTO);
    }
}
