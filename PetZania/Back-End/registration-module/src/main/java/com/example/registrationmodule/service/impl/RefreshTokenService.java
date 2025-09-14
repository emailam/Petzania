package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.model.entity.RevokedRefreshToken;
import com.example.registrationmodule.repository.RevokedRefreshTokenRepository;
import com.example.registrationmodule.service.IRefreshTokenService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class RefreshTokenService implements IRefreshTokenService {
    private final RevokedRefreshTokenRepository revokedRefreshTokenRepository;
    private final JWTService jwtService;
    private static final String CACHE_NAME = "revokedTokens";

    @Override
    @CachePut(value = CACHE_NAME, key = "#token", unless = "#result == null")
    public boolean saveToken(String token) {
        log.info("Saving in the database");
        Date expirationTime = jwtService.extractExpiration(token);
        RevokedRefreshToken revokedRefreshToken = new RevokedRefreshToken();
        revokedRefreshToken.setToken(token);
        revokedRefreshToken.setExpirationTime(expirationTime);

        revokedRefreshTokenRepository.save(revokedRefreshToken);
        return true;
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "#token", unless = "#result == null")
    public boolean isTokenRevoked(String token) {
        log.info("Fetching from database");
        return revokedRefreshTokenRepository.findByToken(token).isPresent();
    }

    @Override
    @Scheduled(cron = "0 0 0 * * ?") // Runs every 24 hours
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteExpiredTokens() {
        revokedRefreshTokenRepository.deleteByExpirationTimeBefore(new Date());
    }
}
