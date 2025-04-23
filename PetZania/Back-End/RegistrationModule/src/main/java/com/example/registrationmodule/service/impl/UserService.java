package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.exception.*;
import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.dto.EmailRequestDTO;
import com.example.registrationmodule.model.entity.RevokedRefreshToken;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.repository.UserRepository;
import com.example.registrationmodule.repository.RevokedRefreshTokenRepository;
import com.example.registrationmodule.service.IDTOConversionService;
import com.example.registrationmodule.service.IEmailService;
import com.example.registrationmodule.service.IUserService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final IDTOConversionService converter;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final IEmailService emailService;
    private final JWTService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${spring.email.sender}")
    private String emailSender;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    @Override
    @RateLimiter(name = "registerRateLimiter", fallbackMethod = "registerFallback")
    public UserProfileDTO registerUser(RegisterUserDTO registerUserDTO) {
        // convert to regular user.
        User user = converter.mapToUser(registerUserDTO);

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new UsernameAlreadyExists("Username '" + user.getUsername() + "' already exists");
        } else if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new EmailAlreadyExists("Email already registered");
        } else {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setVerified(false);
            userRepository.save(user);
        }

        // send verification code.
        sendVerificationCode(user.getEmail());

        return converter.mapToUserProfileDto(user);
    }

    public UserProfileDTO registerFallback(RegisterUserDTO registerUserDTO, RequestNotPermitted t) {
        throw new RuntimeException("Too many registration attempts. Try again later.");
    }

    @Override
    public Page<UserProfileDTO> getUsers(int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return userRepository.findAll(pageable)
                .map(converter::mapToUserProfileDto);
    }

    @Override
    public UserProfileDTO getUserById(UUID userId) {
        return userRepository.findById(userId)
                .map(converter::mapToUserProfileDto)
                .orElseThrow(() -> new UserDoesNotExist("User does not exist"));
    }


    @Override
    public void deleteUser(EmailDTO emailDTO) {
        if (userRepository.findByEmail(emailDTO.getEmail()).isPresent()) {
            sendDeactivationMessage(emailDTO.getEmail());
            userRepository.deleteUserByEmail(emailDTO.getEmail());
        } else {
            throw new UserDoesNotExist("User does not exist");
        }

        System.out.println(emailDTO.getEmail());
        User user = userRepository.findByEmail(emailDTO.getEmail()).orElseThrow(() -> new UserDoesNotExist("User does not exist"));

        // Send delete email
        sendDeleteConfirmation(user);

        // Delete the user
        userRepository.deleteByEmail(emailDTO.getEmail());
    }

    @Override
    public void sendDeleteConfirmation(User user) {
        EmailRequestDTO emailRequestDTO = new EmailRequestDTO();
        emailRequestDTO.setTo(user.getEmail());
        emailRequestDTO.setFrom(emailSender);
        emailRequestDTO.setSubject("Account Deletion Confirmation");
        emailRequestDTO.setBody(
                "Dear " + user.getUsername() + ",\n\n" +
                        "We regret to inform you that your account has been deleted from our system.\n\n" +
                        "If this was a mistake or you have any questions, please contact our support team.\n\n" +
                        "Best regards,\n" +
                        "Petzania Team."
        );

        // send the email
        emailService.sendEmail(emailRequestDTO);
    }

    @Override
    public User saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    @RateLimiter(name = "loginRateLimiter", fallbackMethod = "loginFallback")
    public TokenDTO login(LoginUserDTO loginUserDTO) {
        User user = userRepository.findByEmail(loginUserDTO.getEmail()).orElseThrow(() -> new InvalidUserCredentials("Email is incorrect"));

        if (!user.isVerified()) {
            throw new UserNotVerified("User is not verified");
        }
        if (user.isBlocked()) {
            throw new UserIsBlocked("User is blocked");
        }

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginUserDTO.getEmail(), loginUserDTO.getPassword()));
        if (authentication.isAuthenticated()) {
            return new TokenDTO(jwtService.generateAccessToken(loginUserDTO.getEmail(), "ROLE_USER"), jwtService.generateRefreshToken(loginUserDTO.getEmail(), "ROLE_USER"));
        } else {
            throw new InvalidUserCredentials("Email or password is incorrect");
        }
    }

    public TokenDTO loginFallback(LoginUserDTO loginUserDTO, RequestNotPermitted t) {
        throw new RuntimeException("Too many login attempts. Try again later.");
    }

    @Override
    @RateLimiter(name = "refreshTokenRateLimiter", fallbackMethod = "refreshFallback")
    public TokenDTO refreshToken(String refreshToken) {
        if (refreshToken == null) {
            throw new RefreshTokenNotValid("There is no refresh token sent");
        }
        if (refreshTokenService.isTokenRevoked(refreshToken)) {
            throw new RefreshTokenNotValid("The refresh token is invalid");
        }
        String email = jwtService.extractEmail(refreshToken);
        String role = jwtService.extractRole(refreshToken);
        boolean isExpired = jwtService.isTokenExpired(refreshToken);

        if (email == null || role == null || isExpired) {
            throw new RefreshTokenNotValid("Invalid or expired refresh token");
        }

        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserDoesNotExist("User does not exist"));
        if (user.isBlocked()) {
            throw new UserIsBlocked("User is blocked, sorry cannot able to generate a new token");
        }

        String newAccessToken = jwtService.generateAccessToken(email, role);
        return new TokenDTO(newAccessToken, refreshToken);
    }

    public TokenDTO refreshFallback(String refreshToken, RequestNotPermitted t) {
        throw new RuntimeException("Too many refresh requests. Try again later.");
    }


    @Override
    public void sendResetPasswordOTP(EmailDTO emailDTO) {
        User user = userRepository.findByEmail(emailDTO.getEmail()).orElseThrow(() -> new UserDoesNotExist("User does not exist"));

        if (user.isBlocked()) {
            throw new UserIsBlocked("User is blocked");
        }

        String otp = String.format("%06d", new Random().nextInt(1000000));
        user.setResetCode(otp);
        user.setResetCodeExpirationTime(Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));

        EmailRequestDTO emailRequestDTO = new EmailRequestDTO();
        emailRequestDTO.setTo(user.getEmail());
        emailRequestDTO.setFrom(emailSender);
        emailRequestDTO.setSubject("Reset Password OTP");
        emailRequestDTO.setBody(
                "Dear " + user.getUsername() + ",\n\n" +
                        "To reset your password, please use the following One-Time Password (OTP):\n\n" +
                        "🔐 Your OTP: " + otp + "\n\n" +
                        "This code is valid for 10 minutes.\n\n" +
                        "If you did not request this, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "Petzania Team."
        );

        emailService.sendEmail(emailRequestDTO);
        userRepository.save(user);
    }

    @Override
    public void verifyResetOTP(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserDoesNotExist("User does not exist"));

        if (!otp.equals(user.getResetCode())) {
            throw new InvalidOTPCode("Invalid OTP");
        }

        if (user.getResetCodeExpirationTime().before(Timestamp.valueOf(LocalDateTime.now()))) {
            throw new ExpiredOTP("OTP has expired");
        }

    }

    @Override
    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserDoesNotExist("User does not exist"));

        if (!otp.equals(user.getResetCode())) {
            throw new InvalidOTPCode("Invalid OTP");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetCode(null);
        user.setResetCodeExpirationTime(null);
        userRepository.save(user);
    }


    @Override
    @RateLimiter(name = "logoutRateLimiter", fallbackMethod = "logoutFallback")
    public void logout(LogoutDTO logoutDTO) {
        // get the revoked token data
        User user = userRepository.findByEmail(logoutDTO.getEmail()).orElseThrow(() -> new UserDoesNotExist("User does not exist"));

        if (refreshTokenService.isTokenRevoked(logoutDTO.getRefreshToken())) {
            throw new UserAlreadyLoggedOut("User already logged out");
        }
        // save it in the database
        refreshTokenService.saveToken(logoutDTO.getRefreshToken(), user);
    }

    public void logoutFallback(LogoutDTO logoutDTO, RequestNotPermitted t) {
        throw new RuntimeException("Too many logout requests. Try again later.");
    }

    @Override
    public void blockUser(BlockUserDTO blockUserDTO) {
        User user = userRepository.findByEmail(blockUserDTO.getEmail()).orElseThrow(() -> new UserDoesNotExist("User does not exist"));
        if (user.isBlocked()) {
            throw new UserAlreadyBlocked("User is blocked already");
        } else {
            user.setBlocked(true);
        }
    }

    @Override
    public void unblockUser(BlockUserDTO blockUserDTO) {
        User user = userRepository.findByEmail(blockUserDTO.getEmail()).orElseThrow(() -> new UserDoesNotExist("User does not exist"));
        if (user.isBlocked()) {
            user.setBlocked(false);
        } else {
            throw new UserAlreadyUnblocked(("User is unblocked already"));
        }
    }

    @Override
    public void changePassword(ChangePasswordDTO changePasswordDTO) {
        User user = userRepository.findByEmail(changePasswordDTO.getEmail()).orElseThrow(() -> new UserDoesNotExist("User does not exist"));
        user.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
    }

    @Override
    public void deleteAll() {
        userRepository.deleteAll();
    }

    @Override
    @RateLimiter(name = "verifyOtpRateLimiter", fallbackMethod = "otpFallback")
    public void verifyCode(OTPValidationDTO otpValidationDTO) {
        User user = userRepository.findByEmail(otpValidationDTO.getEmail()).orElseThrow(() -> new UserDoesNotExist("User does not exist"));
        if (user.isVerified()) {
            throw new UserAlreadyVerified("User already verified");
        }
        if (!user.getVerificationCode().equals(otpValidationDTO.getOtp())) {
            throw new InvalidOTPCode("Invalid verification code");
        }
        if (user.getExpirationTime().before(new Timestamp(System.currentTimeMillis()))) {
            throw new ExpiredOTP("Verification code has expired");
        }

        // mark user as verified
        user.setVerified(true);
        user.setVerificationCode(null);
        user.setExpirationTime(null);

        // save the user in the database
        userRepository.save(user);
    }

    public void otpFallback(OTPValidationDTO otpValidationDTO, RequestNotPermitted t) {
        throw new RuntimeException("Too many OTP attempts. Try again later.");
    }

    @Override
    public boolean userExistsById(UUID userId) {
        if (userRepository.findById(userId).isPresent()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    @RateLimiter(name = "sendOtpRateLimiter", fallbackMethod = "sendOtpFallback")
    public void sendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserDoesNotExist("User does not exist"));

        if (user.isVerified()) {
            throw new UserAlreadyVerified("User already verified");
        }
        if (user.isBlocked()) {
            throw new UserIsBlocked("User is blocked");
        }

        System.out.println("User email is: " + email);
        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
        user.setVerificationCode(otp);
        user.setExpirationTime(Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));

        EmailRequestDTO emailRequestDTO = new EmailRequestDTO();
        emailRequestDTO.setTo(user.getEmail());
        emailRequestDTO.setFrom(emailSender);
        emailRequestDTO.setSubject("Your Verification Code");
        emailRequestDTO.setBody(
                "Dear " + user.getUsername() + ",\n\n" +
                        "To complete your verification, please use the following One-Time Password (OTP):\n\n" +
                        "🔑 Your OTP: " + otp + "\n\n" +
                        "This code is valid for **3 minutes**. If you did not request this, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "Petzania Team."
        );

        // send the email.
        emailService.sendEmail(emailRequestDTO);

        // save the user in the database.
        userRepository.save(user);
    }
  
    @Override
    public void sendDeactivationMessage(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserDoesNotExist("User does not exist"));

        EmailRequestDTO emailRequestDTO = new EmailRequestDTO();
        emailRequestDTO.setTo(user.getEmail());
        emailRequestDTO.setFrom(emailSender);
        emailRequestDTO.setSubject("Account Deactivation Notice");
        emailRequestDTO.setBody(
                "Dear " + user.getUsername() + ",\n\n" +
                        "We would like to inform you that your account has been deactivated.\n\n" +
                        "If you believe this was done in error or have any questions, please contact our support team.\n\n" +
                        "Note: You will not be able to access your account until it is reactivated by an administrator.\n\n" +
                        "Best regards,\n" +
                        "Petzania Team."
        );

        emailService.sendEmail(emailRequestDTO);
    }

    public void sendOtpFallback(String email, RequestNotPermitted t) {
        throw new RuntimeException("Too many OTP requests. Try again later.");
    }


    @Override
    public UserProfileDTO updateUserById(UUID userId, UpdateUserProfileDto updateUserProfileDto) {
        if (!userRepository.existsById(userId)) {
            throw new UserDoesNotExist("User does not exist");
        }

        return userRepository.findById(userId).map(existingUser -> {
            Optional.ofNullable(updateUserProfileDto.getName()).ifPresent(existingUser::setName);
            Optional.ofNullable(updateUserProfileDto.getBio()).ifPresent(existingUser::setBio);
            Optional.ofNullable(updateUserProfileDto.getProfilePictureURL()).ifPresent(existingUser::setProfilePictureURL);
            Optional.ofNullable(updateUserProfileDto.getPhoneNumber()).ifPresent(existingUser::setPhoneNumber);

            User updatedUser = userRepository.save(existingUser);
            return converter.mapToUserProfileDto(updatedUser);
        }).orElseThrow(() -> new UserDoesNotExist("User does not exist"));
    }
}
