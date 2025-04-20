package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.model.entity.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@PropertySource("classpath:application.yml")
public class JWTService {
    @Value("${spring.jwt.secret-key}")
    private String secretKey;

    @Value("${spring.access.token.expiration}")
    private long ACCESS_TOKEN_EXPIRATION; // 15 minutes

    @Value("${spring.refresh.token.expiration}")
    private long REFRESH_TOKEN_EXPIRATION; // 1 day


    public String generateAccessToken(String email, String role) {
        return generateToken(email, role, ACCESS_TOKEN_EXPIRATION);
    }

    public String generateRefreshToken(String email, String role) {
        return generateToken(email, role, REFRESH_TOKEN_EXPIRATION);
    }

    public String generateToken(String email, String role, long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey()) // data signature
                .compact();
    }

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

    public boolean validateToken(String token, UserPrincipal userDetails) {
        final String extractedEmail = extractEmail(token);
        if (extractedEmail.equals(userDetails.getEmail()) && !isTokenExpired(token)) {
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
