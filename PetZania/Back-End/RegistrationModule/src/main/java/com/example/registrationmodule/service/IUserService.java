package com.example.registrationmodule.service;

import com.example.registrationmodule.model.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface IUserService {
    Optional<User> getUser(UUID userId);
}
