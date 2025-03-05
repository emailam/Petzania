package com.example.registrationmodule.service;

import com.example.registrationmodule.model.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface IUserService {
    Optional<User> getUserById(UUID userId);
    User saveUser(User user);
    boolean existsById(UUID userId);
    User partialUpdateUserById(UUID userId, User user);
}
