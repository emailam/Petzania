package com.example.notificationmodule.service.impl;

import com.example.notificationmodule.model.principal.AdminPrincipal;
import com.example.notificationmodule.model.principal.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
@Slf4j
@PropertySource("classpath:application.yml")
public class JWTService {
    @Value("${spring.jwt.secret-key}")
    private String secretKey;

    public Key getKey() {
        // Replace with a secure key
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateTokenForUser(String token, UserPrincipal userDetails) {
        final String extractedEmail = extractEmail(token);
        if (extractedEmail.equals(userDetails.getEmail()) && !isTokenExpired(token)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean validateTokenForAdmin(String token, AdminPrincipal adminDetails) {
        final String extractedUsername = extractEmail(token);
        if (extractedUsername.equals(adminDetails.getUsername()) && !isTokenExpired(token)) {
            return true;
        } else {
            return false;
        }
    }


    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            // If the JWT is expired
            log.error("This token {} is expired", token);
            return true;
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}