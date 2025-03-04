package com.example.registrationmodule.service.concrete;

import com.example.registrationmodule.converter.Converter;
import com.example.registrationmodule.exception.EmailAlreadyExistsException;
import com.example.registrationmodule.exception.UserDoesNotExistException;
import com.example.registrationmodule.exception.UsernameAlreadyExistsException;
import com.example.registrationmodule.model.dto.LoginUserDTO;
import com.example.registrationmodule.model.dto.RegisterUserDTO;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.repo.UserRepository;
import com.example.registrationmodule.service.interfaces.IAuthenticationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.beans.Transient;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService implements IAuthenticationService {

    private final Converter converter;
    private final UserRepository userRepository;

    public void registerUser(RegisterUserDTO registerUserDTO) {
        User user = converter.convertToUser(registerUserDTO);
        saveUser(user);
    }

    public void saveUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new UsernameAlreadyExistsException("Username '" + user.getUsername() + "' already exists");
        } else if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already registered");
        } else {
            user.setVerified(false);
            userRepository.save(user);
        }
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }

    public void login(LoginUserDTO loginUserDTO) {
        if (userRepository.findByEmailAndPassword(loginUserDTO.getEmail(), loginUserDTO.getPassword()).isEmpty()) {
            throw new UserDoesNotExistException("Email or Password is invalid");
        } else {
            // log in successful
        }
    }

    public void sendVerificationCode(User user) {
        String otp = String.format("%06", new java.util.Random().nextInt(1000000));
        user.setVerificationCode(otp);
        user.setExpirationTime(Timestamp.valueOf(LocalDateTime.now().plusMinutes(5)));
        // send code to user.
        userRepository.save(user);
    }
}
