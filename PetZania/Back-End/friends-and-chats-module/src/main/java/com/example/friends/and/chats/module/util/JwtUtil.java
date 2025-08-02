package com.example.friends.and.chats.module.util;

import com.example.friends.and.chats.module.service.impl.JWTService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JWTService jwtService;

    public String getUserIdentifierFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "anonymous";
        }

        try {
            String token = authHeader.substring(7);
            return jwtService.extractEmail(token); // Email is the user identifier
        } catch (Exception e) {
            return "anonymous";
        }
    }
}