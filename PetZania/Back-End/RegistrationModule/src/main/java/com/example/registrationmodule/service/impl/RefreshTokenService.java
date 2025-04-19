package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.model.entity.RevokedRefreshToken;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.repository.RevokedRefreshTokenRepository;
import com.example.registrationmodule.service.IRefreshTokenService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.antlr.v4.runtime.Token;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
@Transactional
@AllArgsConstructor
public class RefreshTokenService implements IRefreshTokenService {
    private final RevokedRefreshTokenRepository revokedRefreshTokenRepository;
    private final JWTService jwtService;
    private static final String CACHE_NAME = "revokedTokens";

    @Override
    @CachePut(value = CACHE_NAME, key = "#token", unless = "#result == null")
    public boolean saveToken(String token, User user) {
        System.out.println("Saving in the database");
        Date expirationTime = jwtService.extractExpiration(token);
        RevokedRefreshToken revokedRefreshToken = new RevokedRefreshToken();
        revokedRefreshToken.setUser(user);
        revokedRefreshToken.setToken(token);
        revokedRefreshToken.setExpirationTime(expirationTime);

        revokedRefreshTokenRepository.save(revokedRefreshToken);
        return true;
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "#token", unless = "#result == null")
    public boolean isTokenRevoked(String token) {
        System.out.println("Fetching from database");
        return revokedRefreshTokenRepository.findByToken(token).isPresent();
    }

    @Override
    @Scheduled(cron = "0 0 0 * * ?") // Runs every 24 hours
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteExpiredTokens() {
        revokedRefreshTokenRepository.deleteByExpirationTimeBefore(new Date());
    }
}
