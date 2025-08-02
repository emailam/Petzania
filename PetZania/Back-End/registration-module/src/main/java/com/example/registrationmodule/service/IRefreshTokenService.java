package com.example.registrationmodule.service;

import com.example.registrationmodule.model.entity.RevokedRefreshToken;
import com.example.registrationmodule.model.entity.User;

import java.util.Optional;

public interface IRefreshTokenService {
    boolean saveToken(String token);

    boolean isTokenRevoked(String token);

    void deleteExpiredTokens();
}
