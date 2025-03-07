package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.exception.UserDoesNotExistException;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.model.entity.UserPrincipal;
import com.example.registrationmodule.repo.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Primary
@AllArgsConstructor
public class MyUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // we load user by email but this is the name of the function in the original interface.
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserDoesNotExistException("User does not exist"));
        return new UserPrincipal(user);
    }
}
