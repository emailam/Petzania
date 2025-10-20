package com.example.adoption_and_breeding_module.service.impl;


import com.example.adoption_and_breeding_module.model.principal.AdminPrincipal;
import com.example.adoption_and_breeding_module.model.principal.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
@PropertySource("classpath:application.yml")
public class JWTService {

    @Value("${spring.jwt.secret-key}")
    private String secretKey;

    private final java.time.Clock clock;
    public JWTService() {
        this(java.time.Clock.systemUTC());
    }
    // For tests you can inject a fixed clock
    public JWTService(java.time.Clock clock) {
        this.clock = clock;
    }

    public Key getKey() {// Replace with a secure key
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    public String extractEmail(String token) {
        return extractClaim(token, io.jsonwebtoken.Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    private <T> T extractClaim(String token, java.util.function.Function<io.jsonwebtoken.Claims, T> f) {
        return f.apply(extractAllClaims(token));
    }

    public boolean isTokenExpired(String token) {
        try {
            var exp = extractClaim(token, io.jsonwebtoken.Claims::getExpiration);
            return exp.toInstant().isBefore(java.time.Instant.now(clock));
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return true;
        }
    }

    public boolean validateTokenForUser(String token, com.example.adoption_and_breeding_module.model.principal.UserPrincipal u) {
        return u.getEmail().equals(extractEmail(token)) && !isTokenExpired(token);
    }

    public boolean validateTokenForAdmin(String token, com.example.adoption_and_breeding_module.model.principal.AdminPrincipal a) {
        return a.getUsername().equals(extractEmail(token)) && !isTokenExpired(token);
    }
}