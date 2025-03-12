package com.example.registrationmodule.repository;

import com.example.registrationmodule.model.entity.RevokedRefreshToken;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RevokedRefreshTokenRepository extends JpaRepository<RevokedRefreshToken, UUID> {
    Optional<RevokedRefreshToken> findByToken(String token);

}
