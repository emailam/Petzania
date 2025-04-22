package com.example.registrationmodule.controller;

import com.example.registrationmodule.model.dto.*;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user/auth")
@RequiredArgsConstructor
public class UserController {
    private final IUserService userService;

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
        TokenDTO token = userService.login(loginUserDTO);
        return ResponseEntity.status(HttpStatus.OK).body(new ResponseLoginDTO("Successful login", token));
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

    @PatchMapping("/{id}")
    public ResponseEntity<UserProfileDTO> updateUserProfile(@PathVariable("id") UUID userId,
                                                            @RequestBody UpdateUserProfileDto updateUserProfileDto) {
        UserProfileDTO userProfileDTO = userService.updateUserById(userId, updateUserProfileDto);
        return ResponseEntity.ok(userProfileDTO);
    }

    @PutMapping("/verify")
    public ResponseEntity<String> verifyCode(@RequestBody @Valid OTPValidationDTO otpValidationDTO) {
        userService.verifyCode(otpValidationDTO);
        return ResponseEntity.ok("OTP verification successful");
    }


    @PutMapping("/changePassword")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordDTO changePasswordDTO) {
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
    public ResponseEntity<String> deleteUser(@Valid @RequestBody EmailDTO emailDTO) {
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
            @RequestParam(defaultValue = "id") String sortBy,
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
}
