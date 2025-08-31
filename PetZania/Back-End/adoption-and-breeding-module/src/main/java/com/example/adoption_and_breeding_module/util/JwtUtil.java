package com.example.adoption_and_breeding_module.util;

import com.example.adoption_and_breeding_module.service.impl.JWTService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.example.adoption_and_breeding_module.constant.Constants.*;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JWTService jwtService;

    public String getUserIdentifierFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(STARTING_WITH_STRING)) {
            return ANONYMOUS;
        }

        try {
            String token = authHeader.substring(START_INDEX);
            return jwtService.extractEmail(token); // Email is the user identifier
        } catch (Exception e) {
            return ANONYMOUS;
        }
    }
}
