package com.example.registrationmodule.repository;

import com.example.registrationmodule.model.entity.RevokedRefreshToken;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public interface RevokedRefreshTokenRepository extends JpaRepository<RevokedRefreshToken, UUID> {
    public Optional<RevokedRefreshToken> findByToken(String token);
    public void deleteByExpirationTimeBefore(Date date);

}
