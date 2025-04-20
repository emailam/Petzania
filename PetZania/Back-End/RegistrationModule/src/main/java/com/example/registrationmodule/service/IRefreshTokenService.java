package com.example.registrationmodule.service;

import com.example.registrationmodule.model.entity.RevokedRefreshToken;
import com.example.registrationmodule.model.entity.User;

import java.util.Optional;

public interface IRefreshTokenService {
    public boolean saveToken(String token, User user);

    public boolean isTokenRevoked(String token);

    public void deleteExpiredTokens();
}
