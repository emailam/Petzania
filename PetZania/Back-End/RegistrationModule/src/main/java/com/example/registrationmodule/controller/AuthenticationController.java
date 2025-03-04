package com.example.registrationmodule.controller;

import com.example.registrationmodule.model.dto.LoginUserDTO;
import com.example.registrationmodule.model.dto.RegisterUserDTO;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.service.interfaces.IAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/user/authentication")
@RequiredArgsConstructor
public class AuthenticationController {
    private final IAuthenticationService authenticationService;

    @PostMapping("/signup")
    public void signup(@RequestBody @Valid RegisterUserDTO registerUserDTO) {
        authenticationService.registerUser(registerUserDTO);
    }

    @PostMapping ("/login")
    public void login(@RequestBody @Valid LoginUserDTO loginUserDTO) {
        authenticationService.login(loginUserDTO);
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsers() {
        return ResponseEntity.ok(authenticationService.getUsers());
    }


}
