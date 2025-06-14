package com.example.petzaniasystemtests.utils;

import io.restassured.response.Response;

public class JwtTokenExtractor {
    public static String extractAccessToken(Response response) {
        return response.jsonPath().getString("tokenDTO.accessToken");
    }

    public static String extractRefreshToken(Response response) {
        return response.jsonPath().getString("tokenDTO.refreshToken");
    }

    public static String extractUserId(Response response) {
        return response.jsonPath().getString("userId");
    }
}
