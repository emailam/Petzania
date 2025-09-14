package com.example.registrationmodule.service;

import com.example.registrationmodule.model.entity.AdminPrincipal;
import com.example.registrationmodule.model.entity.UserPrincipal;
import com.example.registrationmodule.service.impl.JWTService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JWTServiceTest {
    private JWTService jwtService;
    private final String secretKeyBase64 = "4ioX3ogCr4GAZ9gB99txf1LyhqVOmTJ8RsL9hyJzZXQ="; // fake secret key

    @BeforeEach
    void setUp() {
        jwtService = new JWTService();
        ReflectionTestUtils.setField(jwtService, "secretKey", secretKeyBase64);
    }
    private String generateToken(Map<String, Object> claims, String subject, long validityMillis) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setExpiration(new Date(System.currentTimeMillis() + validityMillis))
                .signWith(jwtService.getKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    @Test
    void shouldExtractEmail() {
        String subject = "user@example.com";
        String token = generateToken(new HashMap<>(), subject, 10000);

        String extracted = jwtService.extractEmail(token);
        assertEquals(subject, extracted);
    }
    @Test
    void shouldExtractRole() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");
        String token = generateToken(claims, "user@example.com", 10000);

        String extractedRole = jwtService.extractRole(token);
        assertEquals("USER", extractedRole);
    }
    @Test
    void shouldValidateTokenForUser() {
        String subject = "user@example.com";
        String token = generateToken(new HashMap<>(), subject, 10000);
        UserPrincipal mockPrincipal = mock(UserPrincipal.class);
        when(mockPrincipal.getEmail()).thenReturn(subject);

        assertTrue(jwtService.validateTokenForUser(token, mockPrincipal));
    }

    @Test
    void shouldNotValidateTokenWithWrongUser() {
        String subject = "user@example.com";
        String token = generateToken(new HashMap<>(), subject, 10000);
        UserPrincipal mockPrincipal = mock(UserPrincipal.class);
        when(mockPrincipal.getEmail()).thenReturn("wrong@example.com");

        assertFalse(jwtService.validateTokenForUser(token, mockPrincipal));
    }
    @Test
    void shouldValidateTokenForAdmin() {
        String subject = "admin@x.com";
        String token = generateToken(new HashMap<>(), subject, 10000);
        AdminPrincipal mockAdmin = mock(AdminPrincipal.class);
        when(mockAdmin.getUsername()).thenReturn(subject);

        assertTrue(jwtService.validateTokenForAdmin(token, mockAdmin));
    }
    @Test
    void shouldDetectExpiredToken() {
        String subject = "user@expired.com";
        String token = generateToken(new HashMap<>(), subject, -1000); // already expired
        assertTrue(jwtService.isTokenExpired(token));
    }
}
