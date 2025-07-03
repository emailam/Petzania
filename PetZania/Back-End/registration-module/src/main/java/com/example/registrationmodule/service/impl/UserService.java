package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.exception.authenticationAndVerificattion.*;
import com.example.registrationmodule.exception.rateLimiting.*;
import com.example.registrationmodule.exception.user.*;
import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.dto.EmailRequestDTO;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.model.event.UserEvent;
import com.example.registrationmodule.repository.BlockRepository;
import com.example.registrationmodule.repository.UserRepository;
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
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final IDTOConversionService converter;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final BlockRepository blockRepository;
    private final IEmailService emailService;
    private final JWTService jwtService;
    private final AuthenticationManager authenticationManager;

    private final UserPublisher userPublisher;

    @Value("${spring.email.sender}")
    private String emailSender;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    @Override
    @RateLimiter(name = "registerRateLimiter", fallbackMethod = "registerFallback")
    public UserProfileDTO registerUser(RegisterUserDTO registerUserDTO) {
        // convert to regular user.
        User user = converter.mapToUser(registerUserDTO);

        if (userRepository.findByUsernameIgnoreCase(user.getUsername()).isPresent()) {
            throw new UsernameAlreadyExists("Username '" + user.getUsername() + "' already exists");
        } else if (userRepository.findByEmailIgnoreCase(user.getEmail()).isPresent()) {
            throw new EmailAlreadyExists("Email already registered");
        } else {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setVerified(false);
            User savedUser = userRepository.save(user);
            UserEvent userEvent = new UserEvent();
            userEvent.setUserId(savedUser.getUserId());
            userEvent.setUsername(savedUser.getUsername());
            userEvent.setEmail(savedUser.getEmail());
            userPublisher.sendUserRegisteredMessage(userEvent);
        }

        // send verification code.
        sendVerificationCode(user.getEmail());

        return converter.mapToUserProfileDto(user);
    }


    public UserProfileDTO registerFallback(RegisterUserDTO registerUserDTO, RequestNotPermitted t) {
        throw new TooManyRegistrationRequests("Too many registration attempts. Try again later.");
    }

    @Override
    public Page<UserProfileDTO> getUsers(UUID requesterId, int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return userRepository.findAllExcludingBlocked(requesterId, pageable)
                .map(converter::mapToUserProfileDto);
    }

    @Override
    public Page<UserProfileDTO> getUsersByPrefixUsername(UUID requesterId, int page, int size, String sortBy, String direction, String prefix) {
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return userRepository.findByUsernameStartingWithIgnoreCaseExcludingBlocked(prefix, requesterId, pageable)
                .map(converter::mapToUserProfileDto);
    }

    @Override
    public UserProfileDTO getUserById(UUID requesterId, UUID userId) {
        if (blockRepository.existsByBlocker_UserIdAndBlocked_UserId(requesterId, userId) || blockRepository.existsByBlocker_UserIdAndBlocked_UserId(userId, requesterId)) {
            throw new UserAccessDenied("Cannot Perform This Operation Due To Existence Of Blocking");
        }
        return userRepository.findById(userId)
                .map(converter::mapToUserProfileDto)
                .orElseThrow(() -> new UserNotFound("User does not exist"));
    }

    @Override
    public void deleteUser(EmailDTO emailDTO) {
        User user = userRepository.findByEmailIgnoreCase(emailDTO.getEmail()).orElseThrow(() -> new UserNotFound("User does not exist"));

        // Send delete email
        sendDeleteConfirmation(user);

        // Delete the user
        userRepository.deleteByEmail(emailDTO.getEmail());

        // Send to the queue
        UserEvent userEvent = new UserEvent();
        userEvent.setUserId(user.getUserId());
        userEvent.setEmail(user.getEmail());
        userEvent.setUsername(user.getUsername());
        userPublisher.sendUserDeletedMessage(userEvent);
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
        // emailService.sendEmail(emailRequestDTO);

    }

    @Override
    public User saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    @RateLimiter(name = "loginRateLimiter", fallbackMethod = "loginFallback")
    public ResponseLoginDTO login(LoginUserDTO loginUserDTO) {
        User user = userRepository.findByEmailIgnoreCase(loginUserDTO.getEmail()).orElseThrow(() -> new InvalidUserCredentials("Email is incorrect"));

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginUserDTO.getEmail(), loginUserDTO.getPassword()));
        if (authentication.isAuthenticated()) {
            System.out.println("valid credentials");
            if (!user.isVerified()) {
                throw new UserNotVerified("User is not verified");
            }

            if (user.isBlocked()) {
                throw new UserIsBlocked("User is blocked");
            }

            String message = "Successful login";
            TokenDTO tokenDTO = new TokenDTO(jwtService.generateAccessToken(loginUserDTO.getEmail(), "ROLE_USER"),
                    jwtService.generateRefreshToken(loginUserDTO.getEmail(), "ROLE_USER"));
            int loginTimes = user.getLoginTimes();
            UUID userId = user.getUserId();
            user.setLoginTimes(loginTimes + 1);
            user.setOnline(true);
            userRepository.save(user);

            return new ResponseLoginDTO(message, tokenDTO, loginTimes + 1, userId);
        } else {
            System.out.println("invalid credentials");
            throw new InvalidUserCredentials("Email or password is incorrect");
        }
    }

    public ResponseLoginDTO loginFallback(LoginUserDTO loginUserDTO, RequestNotPermitted t) {
        throw new TooManyLoginRequests("Too many login attempts. Try again later.");
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

        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new UserNotFound("User does not exist"));
        if (user.isBlocked()) {
            throw new UserIsBlocked("User is blocked, sorry cannot able to generate a new token");
        }

        String newAccessToken = jwtService.generateAccessToken(email, role);
        return new TokenDTO(newAccessToken, refreshToken);
    }

    public TokenDTO refreshFallback(String refreshToken, RequestNotPermitted t) {
        throw new TooManyRefreshRequests("Too many refresh requests. Try again later.");
    }


    @Override
    public void sendResetPasswordOTP(EmailDTO emailDTO) {
        User user = userRepository.findByEmailIgnoreCase(emailDTO.getEmail()).orElseThrow(() -> new UserNotFound("User does not exist"));

        if (user.isBlocked()) {
            throw new UserIsBlocked("User is blocked");
        }

//        String otp = String.format("%06d", new Random().nextInt(1000000));
        String otp = "123456";
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

        // emailService.sendEmail(emailRequestDTO);
        userRepository.save(user);
    }

    @Override
    public void verifyResetOTP(String email, String otp) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFound("User does not exist"));

        if (!otp.equals(user.getResetCode())) {
            throw new InvalidOTPCode("Invalid OTP");
        }

        if (user.getResetCodeExpirationTime().before(Timestamp.valueOf(LocalDateTime.now()))) {
            throw new ExpiredOTP("OTP has expired");
        }

    }

    @Override
    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFound("User does not exist"));

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
        User user = userRepository.findByEmailIgnoreCase(logoutDTO.getEmail()).orElseThrow(() -> new UserNotFound("User does not exist"));

        if (refreshTokenService.isTokenRevoked(logoutDTO.getRefreshToken())) {
            throw new UserAlreadyLoggedOut("User already logged out");
        }

        // save it in the database
        refreshTokenService.saveToken(logoutDTO.getRefreshToken());

        // mark user as not online
        user.setOnline(false);
        userRepository.save(user);
    }

    public void logoutFallback(LogoutDTO logoutDTO, RequestNotPermitted t) {
        throw new TooManyLogoutRequests("Too many logout requests. Try again later.");
    }

    @Override
    public void blockUser(BlockUserDTO blockUserDTO) {
        User user = userRepository.findByEmailIgnoreCase(blockUserDTO.getEmail()).orElseThrow(() -> new UserNotFound("User does not exist"));
        if (user.isBlocked()) {
            throw new UserAlreadyBlocked("User is blocked already");
        } else {
            user.setBlocked(true);
            sendDeactivationMessage(blockUserDTO.getEmail());
        }
    }

    @Override
    public void unblockUser(BlockUserDTO blockUserDTO) {
        User user = userRepository.findByEmailIgnoreCase(blockUserDTO.getEmail()).orElseThrow(() -> new UserNotFound("User does not exist"));
        if (user.isBlocked()) {
            user.setBlocked(false);
        } else {
            throw new UserAlreadyUnblocked(("User is unblocked already"));
        }
    }

    @Override
    public void changePassword(ChangePasswordDTO changePasswordDTO) {
        User user = userRepository.findByEmailIgnoreCase(changePasswordDTO.getEmail()).orElseThrow(() -> new UserNotFound("User does not exist"));
        user.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
    }

    @Override
    public void deleteAll() {
        List<User> users = userRepository.findAll();
        userRepository.deleteAll();
        for (User user : users) {
            UserEvent userEvent = new UserEvent();
            userEvent.setUserId(user.getUserId());
            userEvent.setUsername(user.getUsername());
            userEvent.setEmail(user.getEmail());
            userPublisher.sendUserDeletedMessage(userEvent);
        }
    }

    @Override
    public ProfilePictureDTO getProfilePictureURLByUserId(UUID requesterId, UUID userId) {
        if (blockRepository.existsByBlocker_UserIdAndBlocked_UserId(requesterId, userId) || blockRepository.existsByBlocker_UserIdAndBlocked_UserId(userId, requesterId)) {
            throw new UserAccessDenied("Cannot Perform This Operation Due To Existence Of Blocking");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFound("User does not exist"));
        ProfilePictureDTO profilePictureDTO = new ProfilePictureDTO();
        profilePictureDTO.setProfilePictureURL(user.getProfilePictureURL());
        return profilePictureDTO;
    }

    @Override
    @RateLimiter(name = "verifyOtpRateLimiter", fallbackMethod = "otpFallback")
    public void verifyCode(OTPValidationDTO otpValidationDTO) {
        User user = userRepository.findByEmailIgnoreCase(otpValidationDTO.getEmail()).orElseThrow(() -> new UserNotFound("User does not exist"));
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
        throw new TooManyOtpRequests("Too many OTP attempts. Try again later.");
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
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFound("User does not exist"));

        if (user.isVerified()) {
            throw new UserAlreadyVerified("User already verified");
        }
        if (user.isBlocked()) {
            throw new UserIsBlocked("User is blocked");
        }

        System.out.println("User email is: " + email);
        // String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
        String otp = "123456";
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
                        "This code is valid for **10 minutes**. If you did not request this, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "Petzania Team."
        );

        // send the email.
        // emailService.sendEmail(emailRequestDTO);

        // save the user in the database.
        userRepository.save(user);
    }

    @Override
    public void sendDeactivationMessage(String email) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new UserNotFound("User does not exist"));

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

        // emailService.sendEmail(emailRequestDTO);
    }

    public void sendOtpFallback(String email, RequestNotPermitted t) {
        throw new TooManyOtpRequests("Too many OTP requests. Try again later.");
    }


    @Override
    public UserProfileDTO updateUserById(UUID userId, UpdateUserProfileDto updateUserProfileDto) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFound("User does not exist");
        }

        return userRepository.findById(userId).map(existingUser -> {
            Optional.ofNullable(updateUserProfileDto.getName()).ifPresent(existingUser::setName);
            Optional.ofNullable(updateUserProfileDto.getBio()).ifPresent(existingUser::setBio);

            String profilePictureURL = updateUserProfileDto.getProfilePictureURL();
            if (profilePictureURL != null) {
                existingUser.setProfilePictureURL(profilePictureURL.isBlank() ? null : profilePictureURL);
            }

            Optional.ofNullable(updateUserProfileDto.getPhoneNumber()).ifPresent(existingUser::setPhoneNumber);

            User updatedUser = userRepository.save(existingUser);
            return converter.mapToUserProfileDto(updatedUser);
        }).orElseThrow(() -> new UserNotFound("User does not exist"));
    }
}
