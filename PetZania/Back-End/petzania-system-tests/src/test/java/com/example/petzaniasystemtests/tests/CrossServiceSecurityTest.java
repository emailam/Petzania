package com.example.petzaniasystemtests.tests;

import com.example.petzaniasystemtests.builders.TestDataBuilder;
import com.example.petzaniasystemtests.config.BaseSystemTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("Cross-Service Security Tests")
public class CrossServiceSecurityTest extends BaseSystemTest {

    @Test
    @DisplayName("Should reject invalid JWT tokens across services")
    void testInvalidTokenRejection() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.token";

        // Registration service should reject
        given()
                .header("Authorization", "Bearer " + invalidToken)
                .when()
                .get(registrationBaseUrl + "/api/user/auth/users")
                .then()
                .statusCode(anyOf(is(401), is(403)));

        // Friends service should reject
        given()
                .header("Authorization", "Bearer " + invalidToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/getFriends")
                .then()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @DisplayName("Should maintain security context across service calls")
    void testSecurityContextPropagation() throws Exception {
        // Register and login two users
        Response user1Response = registerAndLoginUser("secuser1", "secuser1@test.com");
        Response user2Response = registerAndLoginUser("secuser2", "secuser2@test.com");

        String user1Token = user1Response.jsonPath().getString("tokenDTO.accessToken");
        String user2Token = user2Response.jsonPath().getString("tokenDTO.accessToken");
        String user1Id = user1Response.jsonPath().getString("userId");
        String user2Id = user2Response.jsonPath().getString("userId");

        // User1 should not be able to update User2's profile
        given()
                .header("Authorization", "Bearer " + user1Token)
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createUpdateProfileJson("Hacker", "Hacked bio", "+999"))
                .when()
                .patch(registrationBaseUrl + "/api/user/auth/" + user2Id)
                .then()
                .statusCode(anyOf(is(400), is(401)));

        // User1 token should only allow User1 to access their own resources
        given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .get(registrationBaseUrl + "/api/user/auth/" + user1Id)
                .then()
                .statusCode(200)
                .body("userId", equalTo(user1Id));
    }

    @Test
    @DisplayName("Should handle logout affecting both services")
    void testLogoutAcrossServices() throws Exception {
        Response response = registerAndLoginUser("logoutuser", "logoutuser@test.com");
        String token = response.jsonPath().getString("tokenDTO.accessToken");
        String refreshToken = response.jsonPath().getString("tokenDTO.refreshToken");

        // Verify token works in both services
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(registrationBaseUrl + "/api/user/auth/users")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                .then()
                .statusCode(200);

        // Logout from Registration service
        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(String.format("""
                {
                    "email": "logoutuser@test.com",
                    "refreshToken": "%s"
                }
                """, refreshToken))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/logout")
                .then()
                .statusCode(200);

        // Token should still work (JWT is stateless) but refresh should fail
        given()
                .contentType("application/json")
                .body(String.format("{\"refreshToken\": \"%s\"}", refreshToken))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/refresh-token")
                .then()
                .statusCode(anyOf(is(401), is(403)));
    }
}