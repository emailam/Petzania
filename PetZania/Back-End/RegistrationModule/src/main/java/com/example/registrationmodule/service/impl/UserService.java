package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.repository.UserRepository;
import com.example.registrationmodule.service.IUserService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class UserService implements IUserService {

    private final UserRepository userRepository;

    @Override
    public Optional<User> getUser(UUID userId) {
        return userRepository.findById(userId);
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public boolean exists(UUID userId) {
        return userRepository.existsById(userId);
    }

    @Override
    public User updateUser(UUID userId, User updatedUser) {
        return userRepository.findById(userId).map(existingUser -> {
            Optional.ofNullable(updatedUser.getName()).ifPresent(existingUser::setName);
            Optional.ofNullable(updatedUser.getBio()).ifPresent(existingUser::setBio);
            Optional.ofNullable(updatedUser.getProfilePictureURL()).ifPresent(existingUser::setProfilePictureURL);
            Optional.ofNullable(updatedUser.getPhoneNumber()).ifPresent(existingUser::setPhoneNumber);
            return userRepository.save(existingUser);
        }).orElseThrow(() -> new RuntimeException("User does not exist"));
    }
}
