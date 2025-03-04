package com.example.registrationmodule.converter;

import com.example.registrationmodule.model.dto.RegisterUserDTO;
import com.example.registrationmodule.model.entity.User;
import org.springframework.stereotype.Component;

@Component
public class Converter {
    public User convertToUser(RegisterUserDTO registerUserDTO){
        User user = new User();
        user.setUsername(registerUserDTO.getUsername());
        user.setEmail(registerUserDTO.getEmail());
        user.setPassword(registerUserDTO.getPassword());
        return user;
    }
}
