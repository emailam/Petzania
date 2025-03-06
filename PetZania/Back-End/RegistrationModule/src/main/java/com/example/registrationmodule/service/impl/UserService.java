package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.exception.*;
import com.example.registrationmodule.model.dto.LoginUserDTO;
import com.example.registrationmodule.model.dto.OTPValidationDTO;
import com.example.registrationmodule.model.dto.RegisterUserDTO;
import com.example.registrationmodule.model.entity.EmailRequest;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.repo.UserRepository;
import com.example.registrationmodule.service.IDTOConversionService;
import com.example.registrationmodule.service.IEmailService;
import com.example.registrationmodule.service.IUserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final IDTOConversionService converter;
    private final UserRepository userRepository;
    private final IEmailService emailService;

    @Override
    public void registerUser(RegisterUserDTO registerUserDTO) {
        // convert to regular user.
        User user = converter.convertToUser(registerUserDTO);

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new UsernameAlreadyExistsException("Username '" + user.getUsername() + "' already exists");
        } else if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already registered");
        } else {
            user.setVerified(false);
            userRepository.save(user);
        }

        // send verification code.
        sendVerificationCode(user.getUserId());
    }

    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> getUserById(UUID userId) {
        return userRepository.findById(userId);
    }

    @Override
    public void deleteUserById(UUID userId) {
        if (userRepository.findById(userId).isPresent()) {
            userRepository.deleteById(userId);
        } else {
            throw new UserDoesNotExistException("User does not exist");
        }
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public void login(LoginUserDTO loginUserDTO) {
        if (userRepository.findByEmailAndPassword(loginUserDTO.getEmail(), loginUserDTO.getPassword()).isEmpty()) {
            throw new UserDoesNotExistException("Email or Password is invalid");
        }

        User user = userRepository.findByEmailAndPassword(loginUserDTO.getEmail(), loginUserDTO.getPassword()).get();
        if (!user.isVerified()) {
            throw new UserNotVerified("User not verified");
        }
        // user is logged in.
    }

    @Override
    public void deleteAll() {
        userRepository.deleteAll();
    }

    @Override
    public void verifyCode(UUID id, OTPValidationDTO otpValidationDTO) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserDoesNotExistException("User does not exist"));
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

    @Override
    public boolean userExistsByUsername(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            return true;
        } else {
            return false;
        }
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
    public boolean userExistsByEmail(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isUserVerified(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserDoesNotExistException("User does not exist"));
        return user.isVerified();
    }


    @Override
    public void sendVerificationCode(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserDoesNotExistException("User does not exist"));

        if (user.isVerified()) {
            throw new UserAlreadyVerified("User already verified");
        }

        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
        user.setVerificationCode(otp);
        user.setExpirationTime(Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));

        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setTo(user.getEmail());
        emailRequest.setFrom("mohamedkhaledfcai@gmail.com");
        emailRequest.setSubject("Your Verification Code");
        emailRequest.setBody(
                "Dear " + user.getUsername() + ",\n\n" +
                        "To complete your verification, please use the following One-Time Password (OTP):\n\n" +
                        "🔑 Your OTP: " + otp + "\n\n" +
                        "This code is valid for **10 minutes**. If you did not request this, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "Petzania Team."
        );

        // send the email.
        emailService.sendEmail(emailRequest);

        // save the user in the database.
        userRepository.save(user);
    }


    @Override
    public User partialUpdateUserById(UUID userId, User updatedUser) {
        return userRepository.findById(userId).map(existingUser -> {
            Optional.ofNullable(updatedUser.getName()).ifPresent(existingUser::setName);
            Optional.ofNullable(updatedUser.getBio()).ifPresent(existingUser::setBio);
            Optional.ofNullable(updatedUser.getProfilePictureURL()).ifPresent(existingUser::setProfilePictureURL);
            Optional.ofNullable(updatedUser.getPhoneNumber()).ifPresent(existingUser::setPhoneNumber);
            return userRepository.save(existingUser);
        }).orElseThrow(() -> new RuntimeException("User does not exist"));
    }
}
