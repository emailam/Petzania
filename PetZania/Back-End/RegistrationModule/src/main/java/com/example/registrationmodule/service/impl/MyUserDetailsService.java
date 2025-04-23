package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.exception.user.UserNotFound;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.model.entity.UserPrincipal;
import com.example.registrationmodule.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Primary
@AllArgsConstructor
@Transactional
public class MyUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username).orElseThrow(() -> new UserNotFound("Username does not exist"));
        return new UserPrincipal(user);
    }

    public UserDetails loadUserByEmail(String email) throws UserNotFound {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFound("Email does not exist"));
        return new UserPrincipal(user);
    }
}
