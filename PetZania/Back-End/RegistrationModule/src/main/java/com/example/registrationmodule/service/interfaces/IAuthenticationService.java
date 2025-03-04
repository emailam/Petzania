package com.example.registrationmodule.service.interfaces;

import com.example.registrationmodule.converter.Converter;
import com.example.registrationmodule.model.dto.LoginUserDTO;
import com.example.registrationmodule.model.dto.RegisterUserDTO;
import com.example.registrationmodule.model.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface IAuthenticationService {
    public void registerUser(RegisterUserDTO registerUserDTO);

    @Transactional
    public void saveUser(User user);

    @Transactional
    public void sendVerificationCode(User user);

    @Transactional
    public List<User> getUsers();

    void login(LoginUserDTO loginUserDTO);
}
