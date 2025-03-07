package com.example.registrationmodule.service;

import com.example.registrationmodule.model.dto.LoginUserDTO;
import com.example.registrationmodule.model.dto.OTPValidationDTO;
import com.example.registrationmodule.model.dto.RegisterUserDTO;
import com.example.registrationmodule.model.entity.EmailRequest;
import com.example.registrationmodule.model.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Transactional
public interface IUserService {
    public void registerUser(RegisterUserDTO registerUserDTO);

    public void sendVerificationCode(UUID userID);

    public List<User> getUsers();


    public Optional<User> getUserById(UUID userId);

    public void deleteUserById(UUID userId);
    public User saveUser(User user);

    String login(LoginUserDTO loginUserDTO);

    void deleteAll();

    void verifyCode(UUID id, OTPValidationDTO otpValidationDTO);

    boolean userExistsByUsername(String username);

    boolean userExistsById(UUID userId);

    boolean userExistsByEmail(String email);

    boolean isUserVerified(UUID userId);
    User partialUpdateUserById(UUID userId, User user);
}
