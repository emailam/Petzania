package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.exception.UserDoesNotExistException;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.model.entity.UserPrincipal;
import com.example.registrationmodule.repo.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
@AllArgsConstructor
public class MyUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username).orElseThrow(() -> new UserDoesNotExistException("Username does not exist"));
        return new UserPrincipal(user);
    }

    public UserDetails loadUserByEmail(String email) throws UserDoesNotExistException {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserDoesNotExistException("Email does not exist"));
        return new UserPrincipal(user);
    }
}
