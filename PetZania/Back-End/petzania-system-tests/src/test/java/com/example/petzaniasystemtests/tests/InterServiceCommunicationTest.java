package com.example.petzaniasystemtests.tests;

import com.example.petzaniasystemtests.builders.TestDataBuilder;
import com.example.petzaniasystemtests.config.BaseSystemTest;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Inter-Service Communication Tests between Registration and Friends/Chat Services")
public class InterServiceCommunicationTest extends BaseSystemTest {

    private static String userToken;
    private static UUID userId;
    private static final String USER_EMAIL = "interservice@test.com";
    private static final String PASSWORD = "Password123!";

    @Test
    @Order(1)
    @DisplayName("Should propagate user registration event from Registration to Friends service via RabbitMQ")
    void testUserRegistrationEventPropagation() throws Exception {
        // Step 1: Register user in Registration service
        Response registrationResponse = given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createRegisterUserJson("interserviceuser", USER_EMAIL, PASSWORD))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/signup")
                .then()
                .statusCode(201)
                .extract().response();

        userId = UUID.fromString(registrationResponse.jsonPath().getString("userProfileDTO.userId"));

        // Step 2: Get OTP from database
        String otp = getOtpFromDatabase(USER_EMAIL);
        Assertions.assertNotNull(otp, "OTP should be generated and stored in database");

        // Step 3: Verify OTP and Login to get token
        given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createOtpValidationJson(USER_EMAIL, otp))
                .when()
                .put(registrationBaseUrl + "/api/user/auth/verify")
                .then()
                .statusCode(200);

        Response loginResponse = given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createLoginJson(USER_EMAIL, PASSWORD))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        userToken = loginResponse.jsonPath().getString("tokenDTO.accessToken");

        // Step 3: Wait and verify user exists in Friends service by attempting to access friend endpoints
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    // Try to get friends list - should work if user was propagated
                    given()
                            .header("Authorization", "Bearer " + userToken)
                            .when()
                            .get(friendsBaseUrl + "/api/friends/getFriends")
                            .then()
                            .statusCode(204); // No content but request should be authorized
                });
    }

    @Test
    @Order(2)
    @DisplayName("Should validate JWT token from Registration service in Friends service")
    void testCrossServiceJWTValidation() {
        // Use token from Registration service to access Friends service endpoints
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                .then()
                .statusCode(200)
                .body(equalTo("0"));

        // Test multiple endpoints to ensure JWT works across services
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/getFollowers")
                .then()
                .statusCode(204); // No followers yet

        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(friendsBaseUrl + "/api/chats")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(3)
    @DisplayName("Should handle user blocking across services")
    void testUserBlockingAcrossServices() throws Exception {
        // Create a second user
        String user2Email = "blocked@test.com";
        Response user2Response = given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createRegisterUserJson("blockeduser", user2Email, PASSWORD))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/signup")
                .then()
                .statusCode(201)
                .extract().response();

        UUID user2Id = UUID.fromString(user2Response.jsonPath().getString("userProfileDTO.userId"));

        // Get OTP and verify user2
        String user2Otp = getOtpFromDatabase(user2Email);
        given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createOtpValidationJson(user2Email, user2Otp))
                .when()
                .put(registrationBaseUrl + "/api/user/auth/verify")
                .then()
                .statusCode(200);

        Response user2LoginResponse = given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createLoginJson(user2Email, PASSWORD))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/login")
                .then()
                .extract().response();

        String user2Token = user2LoginResponse.jsonPath().getString("tokenDTO.accessToken");

        // Wait for user propagation to Friends service
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    given()
                            .header("Authorization", "Bearer " + user2Token)
                            .when()
                            .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                            .then()
                            .statusCode(200);
                });

        // User1 blocks User2 in Friends service
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .post(friendsBaseUrl + "/api/friends/block/" + user2Id)
                .then()
                .statusCode(201);

        // Verify User2 cannot send friend request to User1
        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + userId)
                .then()
                .statusCode(anyOf(is(400), is(403), is(409))); // Should be blocked
    }

    @Test
    @Order(4)
    @DisplayName("Should sync user data when creating friendship across services")
    void testUserDataSyncDuringFriendship() throws Exception {
        // Create two fresh users
        String user3Email = "friend1@test.com";
        String user4Email = "friend2@test.com";

        // Register and login both users
        Response user3Response = registerAndLoginUser("friend1", user3Email);
        Response user4Response = registerAndLoginUser("friend2", user4Email);

        String user3Token = user3Response.jsonPath().getString("tokenDTO.accessToken");
        String user4Token = user4Response.jsonPath().getString("tokenDTO.accessToken");
        UUID user3Id = UUID.fromString(user3Response.jsonPath().getString("userId"));
        UUID user4Id = UUID.fromString(user4Response.jsonPath().getString("userId"));

        // Wait for propagation
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    given()
                            .header("Authorization", "Bearer " + user3Token)
                            .when()
                            .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                            .then()
                            .statusCode(200);
                });

        // User3 sends friend request to User4
        Response friendRequestResponse = given()
                .header("Authorization", "Bearer " + user3Token)
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user4Id)
                .then()
                .statusCode(201)
                .body("sender.userId", equalTo(user3Id.toString()))
                .body("receiver.userId", equalTo(user4Id.toString()))
                .extract().response();

        UUID requestId = UUID.fromString(friendRequestResponse.jsonPath().getString("requestId"));

        // User4 accepts friend request
        given()
                .header("Authorization", "Bearer " + user4Token)
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + requestId)
                .then()
                .statusCode(201)
                .body("user1.userId", notNullValue())
                .body("user2.userId", notNullValue());

        // Verify both users can see each other as friends
        given()
                .header("Authorization", "Bearer " + user3Token)
                .when()
                .get(friendsBaseUrl + "/api/friends/getFriends")
                .then()
                .statusCode(200)
                .body("content", hasSize(1));

        given()
                .header("Authorization", "Bearer " + user4Token)
                .when()
                .get(friendsBaseUrl + "/api/friends/getFriends")
                .then()
                .statusCode(200)
                .body("content", hasSize(1));
    }

    @Test
    @Order(5)
    @DisplayName("Should create chat between users registered in different order")
    void testChatCreationWithUserSync() throws Exception {
        // This tests that users registered at different times can interact
        String user5Email = "chatuser1@test.com";
        String user6Email = "chatuser2@test.com";

        // Register first user
        Response user5Response = registerAndLoginUser("chatuser1", user5Email);
        String user5Token = user5Response.jsonPath().getString("tokenDTO.accessToken");
        UUID user5Id = UUID.fromString(user5Response.jsonPath().getString("userId"));

        // Wait a bit, then register second user
        Thread.sleep(2000);

        Response user6Response = registerAndLoginUser("chatuser2", user6Email);
        String user6Token = user6Response.jsonPath().getString("tokenDTO.accessToken");
        UUID user6Id = UUID.fromString(user6Response.jsonPath().getString("userId"));

        // Wait for both users to be synced
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    given()
                            .header("Authorization", "Bearer " + user5Token)
                            .when()
                            .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                            .then()
                            .statusCode(200);
                });

        // User5 creates chat with User6
        Response chatResponse = given()
                .header("Authorization", "Bearer " + user5Token)
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + user6Id)
                .then()
                .statusCode(201)
                .body("user1Id", anyOf(equalTo(user5Id.toString()), equalTo(user6Id.toString())))
                .body("user2Id", anyOf(equalTo(user5Id.toString()), equalTo(user6Id.toString())))
                .extract().response();

        UUID chatId = UUID.fromString(chatResponse.jsonPath().getString("chatId"));

        // Verify both users can access the chat
        given()
                .header("Authorization", "Bearer " + user5Token)
                .when()
                .get(friendsBaseUrl + "/api/chats/" + chatId)
                .then()
                .statusCode(200)
                .body("chatId", equalTo(chatId.toString()));

        given()
                .header("Authorization", "Bearer " + user6Token)
                .when()
                .get(friendsBaseUrl + "/api/chats/" + chatId)
                .then()
                .statusCode(200)
                .body("chatId", equalTo(chatId.toString()));

        // Test sending a message
        given()
                .header("Authorization", "Bearer " + user5Token)
                .contentType("application/json")
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(chatId, "Hello from user5!"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200)
                .body("content", equalTo("Hello from user5!"))
                .body("chatId", equalTo(chatId.toString()))
                .body("senderId", equalTo(user5Id.toString()));
    }

    @Test
    @Order(6)
    @DisplayName("Should handle JWT token refresh across services")
    void testTokenRefreshAcrossServices() throws Exception {
        String testEmail = "tokentest@test.com";
        Response response = registerAndLoginUser("tokentest", testEmail);
        String accessToken = response.jsonPath().getString("tokenDTO.accessToken");
        String refreshToken = response.jsonPath().getString("tokenDTO.refreshToken");

        // Use the token in Friends service
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                .then()
                .statusCode(200);

        // Refresh token in Registration service
        Response refreshResponse = given()
                .contentType("application/json")
                .body(String.format("{\"refreshToken\": \"%s\"}", refreshToken))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/refresh-token")
                .then()
                .statusCode(200)
                .extract().response();

        String newAccessToken = refreshResponse.jsonPath().getString("accessToken");

        // Verify new token works in Friends service
        given()
                .header("Authorization", "Bearer " + newAccessToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(7)
    @DisplayName("Should handle concurrent user operations across services")
    void testConcurrentUserOperations() throws Exception {
        // Create multiple users concurrently
        String[] emails = {"concurrent1@test.com", "concurrent2@test.com", "concurrent3@test.com"};
        Response[] responses = new Response[3];

        // Register all users
        for (int i = 0; i < emails.length; i++) {
            responses[i] = registerAndLoginUser("concurrent" + (i + 1), emails[i]);
        }

        String user1Token = responses[0].jsonPath().getString("tokenDTO.accessToken");
        String user2Token = responses[1].jsonPath().getString("tokenDTO.accessToken");
        UUID user1Id = UUID.fromString(responses[0].jsonPath().getString("userId"));
        UUID user2Id = UUID.fromString(responses[1].jsonPath().getString("userId"));
        UUID user3Id = UUID.fromString(responses[2].jsonPath().getString("userId"));

        // Wait for all users to be synced
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    for (Response resp : responses) {
                        String token = resp.jsonPath().getString("tokenDTO.accessToken");
                        given()
                                .header("Authorization", "Bearer " + token)
                                .when()
                                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                                .then()
                                .statusCode(200);
                    }
                });

        // User1 sends friend requests to both User2 and User3
        given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user2Id)
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user3Id)
                .then()
                .statusCode(201);

        // Verify User1 cannot send duplicate request
        given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user2Id)
                .then()
                .statusCode(anyOf(is(400), is(409)));

        // User2 blocks User1
        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .post(friendsBaseUrl + "/api/friends/block/" + user1Id)
                .then()
                .statusCode(201);

        // Verify User1 is blocked
        given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + user2Id)
                .then()
                .statusCode(anyOf(is(400), is(403)));
    }

}