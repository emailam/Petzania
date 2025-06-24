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

    private Key getKey() {
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
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
