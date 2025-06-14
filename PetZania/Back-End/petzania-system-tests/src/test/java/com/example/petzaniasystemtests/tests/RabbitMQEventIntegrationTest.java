package com.example.petzaniasystemtests.tests;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.UUID;

import com.example.petzaniasystemtests.builders.TestDataBuilder;

import com.example.petzaniasystemtests.config.BaseSystemTest;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;

@DisplayName("RabbitMQ Event Integration Tests")
public class RabbitMQEventIntegrationTest extends BaseSystemTest {

    @Test
    @DisplayName("Should handle rapid user registrations and ensure all events are processed")
    void testRapidUserRegistrationEvents() throws Exception {
        // Register multiple users rapidly
        int userCount = 5;
        String[] userEmails = new String[userCount];
        String[] userTokens = new String[userCount];
        UUID[] userIds = new UUID[userCount];

        // Register users quickly
        for (int i = 0; i < userCount; i++) {
            userEmails[i] = String.format("rapiduser%d@test.com", i);
            Response response = given()
                    .contentType("application/json")
                    .body(TestDataBuilder.UserBuilder.createRegisterUserJson(
                            "rapiduser" + i, userEmails[i], "Password123!"))
                    .when()
                    .post(registrationBaseUrl + "/api/user/auth/signup")
                    .then()
                    .statusCode(201)
                    .extract().response();

            userIds[i] = UUID.fromString(response.jsonPath().getString("userProfileDTO.userId"));
        }

        // Verify all users and login
        for (int i = 0; i < userCount; i++) {
            String otp = getOtpFromDatabase(userEmails[i]);
            given()
                    .contentType("application/json")
                    .body(TestDataBuilder.UserBuilder.createOtpValidationJson(userEmails[i], otp))
                    .when()
                    .put(registrationBaseUrl + "/api/user/auth/verify");

            Response loginResponse = given()
                    .contentType("application/json")
                    .body(TestDataBuilder.UserBuilder.createLoginJson(userEmails[i], "Password123!"))
                    .when()
                    .post(registrationBaseUrl + "/api/user/auth/login")
                    .then()
                    .extract().response();

            userTokens[i] = loginResponse.jsonPath().getString("tokenDTO.accessToken");
        }

        // Verify all users can access Friends service (proving events were processed)
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    for (int i = 0; i < userCount; i++) {
                        given()
                                .header("Authorization", "Bearer " + userTokens[i])
                                .when()
                                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                                .then()
                                .statusCode(200);
                    }
                });
    }
}