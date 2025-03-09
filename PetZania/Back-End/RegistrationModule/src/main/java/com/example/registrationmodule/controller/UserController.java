package com.example.registrationmodule.controller;

import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.EmailRequest;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.service.IDTOConversionService;
import com.example.registrationmodule.service.IEmailService;
import com.example.registrationmodule.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/user/auth")
@RequiredArgsConstructor
public class UserController {
    private final IUserService userService;
    private final IDTOConversionService dtoConversionService;


    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody @Valid RegisterUserDTO registerUserDTO) {
        if (userService.userExistsByUsername(registerUserDTO.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
        } else if (userService.userExistsByEmail(registerUserDTO.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
        } else {
            userService.registerUser(registerUserDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
        }
    }

    @PatchMapping(path = "/user/{id}")
    public ResponseEntity<UserProfileDTO> partialUpdateUserProfileById(@PathVariable("id") UUID userId,
                                                                       @RequestBody UpdateUserProfileDto updateUserProfileDto) {

        if (!userService.userExistsById(userId)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        User user = dtoConversionService.mapToUser(updateUserProfileDto);
        User updatedUser = userService.partialUpdateUserById(userId, user);
        return new ResponseEntity<>(
                dtoConversionService.mapToUserProfileDto(updatedUser),
                HttpStatus.OK
        );
    }

    @PutMapping("/resendOTP/{userId}")
    public ResponseEntity<String> resendOTP(@PathVariable UUID userId) {
        if (userService.isUserVerified(userId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already verified");
        } else {
            userService.sendVerificationCode(userId);
            return ResponseEntity.ok("A new OTP was sent");
        }
    }

    @PutMapping("/verify/{userId}")
    public ResponseEntity<String> verifyCode(@PathVariable UUID userId, @RequestBody @Valid OTPValidationDTO otpValidationDTO) {
        if (userService.isUserVerified(userId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already verified");
        } else {
            userService.verifyCode(userId, otpValidationDTO);
            return ResponseEntity.ok("OTP verification successful");
        }
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<String> deleteUserById(@PathVariable UUID userId) {
        if (!userService.userExistsById(userId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User does not exist");
        } else {
            userService.deleteUserById(userId);
            return ResponseEntity.ok("User deleted successfully");
        }
    }

    @DeleteMapping("/deleteAll")
    public ResponseEntity<String> deleteAllUsers() {
        userService.deleteAll();
        return ResponseEntity.ok("All users deleted successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody @Valid LoginUserDTO loginUserDTO) {
        if (!userService.userExistsByEmail(loginUserDTO.getEmail())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email does not exist");
        } else if (!userService.isUserVerified(loginUserDTO.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User is not verified");
        } else {
            String token = userService.login(loginUserDTO);
            return ResponseEntity.status(HttpStatus.OK).body("User logged in successfully, User token: " + token);
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsers() {
        List<User> users = userService.getUsers();
        if (users.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(users);
        }
        return ResponseEntity.ok(users);
    }

    @GetMapping(path = "/user/{id}")
    public ResponseEntity<UserProfileDTO> getUserById(@PathVariable("id") UUID userId) {
        Optional<User> user = userService.getUserById(userId);
        return user.map(userEntity -> {
            UserProfileDTO userProfileDto = dtoConversionService.mapToUserProfileDto(userEntity);
            return new ResponseEntity<>(userProfileDto, HttpStatus.OK);
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
