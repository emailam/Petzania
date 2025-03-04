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
}
