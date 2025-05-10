package com.example.friends.and.chats.module.service.impl;


import com.example.friends.and.chats.module.exception.admin.AdminNotFound;
import com.example.friends.and.chats.module.exception.user.UserNotFound;
import com.example.friends.and.chats.module.model.entity.Admin;
import com.example.friends.and.chats.module.model.entity.User;
import com.example.friends.and.chats.module.model.principal.AdminPrincipal;
import com.example.friends.and.chats.module.model.principal.UserPrincipal;
import com.example.friends.and.chats.module.repository.AdminRepository;
import com.example.friends.and.chats.module.repository.UserRepository;
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
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            return loadUserByEmail(username);
        } catch (UserNotFound e) {
            try {
                return loadAdminByUsername(username);
            } catch (AdminNotFound ex) {
                throw new UsernameNotFoundException("User or admin not found");
            }
        }
    }

    public UserDetails loadAdminByUsername(String username) throws AdminNotFound {
        Admin admin = adminRepository.findByUsernameIgnoreCase(username).orElseThrow(() -> new AdminNotFound("Admin does not exist"));
        return new AdminPrincipal(admin);
    }

    public UserDetails loadUserByEmail(String email) throws UserNotFound {
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new UserNotFound("Email does not exist"));
        return new UserPrincipal(user);
    }

}
