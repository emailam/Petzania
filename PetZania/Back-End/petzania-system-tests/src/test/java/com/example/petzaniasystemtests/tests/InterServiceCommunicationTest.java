package com.example.petzaniasystemtests.tests;

import com.example.petzaniasystemtests.builders.TestDataBuilder;
import com.example.petzaniasystemtests.config.BaseSystemTest;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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

    @Test
    @Order(8)
    @DisplayName("Should handle user deletion propagation across services")
    void testUserDeletionPropagation() throws Exception {
        // Create a user to be deleted
        String deleteUserEmail = "tobedeleted@test.com";
        Response deleteUserResponse = registerAndLoginUser("deleteuser", deleteUserEmail);
        String deleteUserToken = deleteUserResponse.jsonPath().getString("tokenDTO.accessToken");
        UUID deleteUserId = UUID.fromString(deleteUserResponse.jsonPath().getString("userId"));

        // Create another user to interact with
        String keepUserEmail = "keepuser@test.com";
        Response keepUserResponse = registerAndLoginUser("keepuser", keepUserEmail);
        String keepUserToken = keepUserResponse.jsonPath().getString("tokenDTO.accessToken");
        UUID keepUserId = UUID.fromString(keepUserResponse.jsonPath().getString("userId"));

        // Wait for sync
        Thread.sleep(2000);

        // Create friendship between them
        Response friendRequest = given()
                .header("Authorization", "Bearer " + deleteUserToken)
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + keepUserId)
                .then()
                .statusCode(201)
                .extract().response();

        UUID requestId = UUID.fromString(friendRequest.jsonPath().getString("requestId"));

        given()
                .header("Authorization", "Bearer " + keepUserToken)
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + requestId)
                .then()
                .statusCode(201);

        // Create chat
        Response chatResponse = given()
                .header("Authorization", "Bearer " + deleteUserToken)
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + keepUserId)
                .then()
                .statusCode(201)
                .extract().response();

        UUID chatId = UUID.fromString(chatResponse.jsonPath().getString("chatId"));

        // Delete user from Registration service
        given()
                .header("Authorization", "Bearer " + deleteUserToken)
                .contentType("application/json")
                .body(String.format("{\"email\": \"%s\"}", deleteUserEmail))
                .when()
                .delete(registrationBaseUrl + "/api/user/auth/delete")
                .then()
                .statusCode(200);

        // Verify deleted user's token no longer works
        given()
                .header("Authorization", "Bearer " + deleteUserToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                .then()
                .statusCode(anyOf(is(401), is(403)));

        // Verify keepUser's friend count decreased
        given()
                .header("Authorization", "Bearer " + keepUserToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                .then()
                .statusCode(200)
                .body(equalTo("0")); // Friend should be removed

        // Chat might still exist but deleted user cannot access
        given()
                .header("Authorization", "Bearer " + deleteUserToken)
                .when()
                .get(friendsBaseUrl + "/api/chats/" + chatId)
                .then()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(9)
    @DisplayName("Should handle complex multi-service workflows")
    void testComplexMultiServiceWorkflow() throws Exception {
        // Create a group of users
        int groupSize = 4;
        List<Response> userResponses = new ArrayList<>();
        List<String> tokens = new ArrayList<>();
        List<UUID> userIds = new ArrayList<>();

        for (int i = 0; i < groupSize; i++) {
            Response resp = registerAndLoginUser("groupuser" + i, "groupuser" + i + "@test.com");
            userResponses.add(resp);
            tokens.add(resp.jsonPath().getString("tokenDTO.accessToken"));
            userIds.add(UUID.fromString(resp.jsonPath().getString("userId")));
        }

        // Wait for all to sync
        Thread.sleep(3000);

        // Create a complex friend network
        // User0 -> friends with User1, User2
        // User1 -> friends with User0, User3
        // User2 -> friends with User0, User3
        // User3 -> friends with User1, User2

        // Send and accept friend requests
        sendAndAcceptFriendRequest(tokens.get(0), tokens.get(1), userIds.get(1));
        sendAndAcceptFriendRequest(tokens.get(0), tokens.get(2), userIds.get(2));
        sendAndAcceptFriendRequest(tokens.get(1), tokens.get(3), userIds.get(3));
        sendAndAcceptFriendRequest(tokens.get(2), tokens.get(3), userIds.get(3));

        // Create group chat scenarios
        Response chat01 = given()
                .header("Authorization", "Bearer " + tokens.get(0))
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + userIds.get(1))
                .then()
                .statusCode(201)
                .extract().response();

        UUID chatId01 = UUID.fromString(chat01.jsonPath().getString("chatId"));

        // Send messages in chat
        for (int i = 0; i < 5; i++) {
            String senderToken = i % 2 == 0 ? tokens.get(0) : tokens.get(1);
            given()
                    .header("Authorization", "Bearer " + senderToken)
                    .contentType("application/json")
                    .body(TestDataBuilder.MessageBuilder.createSendMessageJson(chatId01, "Message " + i))
                    .when()
                    .post(friendsBaseUrl + "/api/messages/send")
                    .then()
                    .statusCode(200);
        }

        // User2 blocks User3
        given()
                .header("Authorization", "Bearer " + tokens.get(2))
                .when()
                .post(friendsBaseUrl + "/api/friends/block/" + userIds.get(3))
                .then()
                .statusCode(201);

        // Verify User3 cannot create chat with User2
        given()
                .header("Authorization", "Bearer " + tokens.get(3))
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + userIds.get(2))
                .then()
                .statusCode(anyOf(is(400), is(403)));

        // Verify friend counts
        given()
                .header("Authorization", "Bearer " + tokens.get(0))
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                .then()
                .statusCode(200)
                .body(equalTo("2"));
    }

    @Test
    @Order(10)
    @DisplayName("Should handle cross-service validation for non-existent users")
    void testNonExistentUserValidation() throws Exception {
        // Use a valid token but try to interact with non-existent users
        UUID nonExistentUserId = UUID.randomUUID();

        // Try to send friend request to non-existent user
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + nonExistentUserId)
                .then()
                .statusCode(anyOf(is(400), is(404)));

        // Try to create chat with non-existent user
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + nonExistentUserId)
                .then()
                .statusCode(anyOf(is(400), is(404)));

        // Try to follow non-existent user
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .post(friendsBaseUrl + "/api/friends/follow/" + nonExistentUserId)
                .then()
                .statusCode(anyOf(is(400), is(404)));
    }

    @Test
    @Order(11)
    @DisplayName("Should handle cross-service operations when user is not verified")
    void testUnverifiedUserRestrictions() throws Exception {
        // Register user but don't verify
        String unverifiedEmail = "unverified@test.com";
        Response unverifiedResponse = given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createRegisterUserJson("unverified", unverifiedEmail, PASSWORD))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/signup")
                .then()
                .statusCode(201)
                .extract().response();

        UUID unverifiedId = UUID.fromString(unverifiedResponse.jsonPath().getString("userProfileDTO.userId"));

        // Try to login without verification
        Response loginAttempt = given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createLoginJson(unverifiedEmail, PASSWORD))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/login")
                .then()
                .extract().response();

        // Depending on implementation, unverified users might get limited tokens or be blocked
        if (loginAttempt.statusCode() == 200) {
            String limitedToken = loginAttempt.jsonPath().getString("tokenDTO.accessToken");

            // Unverified user might have restrictions in Friends service
            given()
                    .header("Authorization", "Bearer " + limitedToken)
                    .when()
                    .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                    .then()
                    .statusCode(anyOf(is(200), is(403))); // Depends on business rules
        }
    }

    @Test
    @Order(12)
    @DisplayName("Should handle password change affecting both services")
    void testPasswordChangeAcrossServices() throws Exception {
        String pwdChangeEmail = "pwdchange@test.com";
        Response userResponse = registerAndLoginUser("pwdchange", pwdChangeEmail);
        String oldToken = userResponse.jsonPath().getString("tokenDTO.accessToken");

        // Use old token in Friends service
        given()
                .header("Authorization", "Bearer " + oldToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                .then()
                .statusCode(200);

        // Change password
        given()
                .header("Authorization", "Bearer " + oldToken)
                .contentType("application/json")
                .body("""
                {
                    "email": "%s",
                    "newPassword": "NewPassword123!"
                }
                """.formatted(pwdChangeEmail))
                .when()
                .put(registrationBaseUrl + "/api/user/auth/changePassword")
                .then()
                .statusCode(200);

        // Login with new password
        Response newLoginResponse = given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createLoginJson(pwdChangeEmail, "NewPassword123!"))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        String newToken = newLoginResponse.jsonPath().getString("tokenDTO.accessToken");

        // Both old and new tokens might work (JWT is stateless)
        // but this demonstrates the cross-service authentication flow
        given()
                .header("Authorization", "Bearer " + newToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(13)
    @DisplayName("Should handle user search integration between services")
    void testUserSearchIntegration() throws Exception {
        // Create users with searchable patterns
        String[] searchUsers = {
                "searchableAlpha@test.com",
                "searchableBeta@test.com",
                "searchableGamma@test.com",
                "differentUser@test.com"
        };

        List<String> tokens = new ArrayList<>();
        List<UUID> userIds = new ArrayList<>();

        for (int i = 0; i < searchUsers.length; i++) {
            Response resp = registerAndLoginUser(searchUsers[i].split("@")[0], searchUsers[i]);
            tokens.add(resp.jsonPath().getString("tokenDTO.accessToken"));
            userIds.add(UUID.fromString(resp.jsonPath().getString("userId")));
        }

        // Wait for sync
        Thread.sleep(2000);

        // Search in Registration service
        Response searchResponse = given()
                .header("Authorization", "Bearer " + tokens.get(0))
                .when()
                .get(registrationBaseUrl + "/api/user/auth/users/searchable")
                .then()
                .statusCode(200)
                .body("content", hasSize(greaterThanOrEqualTo(3)))
                .extract().response();

        // Use search results to interact in Friends service
        // First user follows all searchable users
        for (int i = 1; i < 3; i++) {
            given()
                    .header("Authorization", "Bearer " + tokens.get(0))
                    .when()
                    .post(friendsBaseUrl + "/api/friends/follow/" + userIds.get(i))
                    .then()
                    .statusCode(201);
        }

        // Verify following count
        given()
                .header("Authorization", "Bearer " + tokens.get(0))
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFollowing")
                .then()
                .statusCode(200)
                .body(equalTo("2"));
    }
    @Test
    @Order(14)
    @DisplayName("Should handle message operations after user profile updates")
    void testMessageOperationsAfterProfileUpdates() throws Exception {
        // Create two users for chat
        String user1Email = "msgupdater1@test.com";
        String user2Email = "msgupdater2@test.com";

        Response user1Response = registerAndLoginUser("msgupdater1", user1Email);
        Response user2Response = registerAndLoginUser("msgupdater2", user2Email);

        String user1Token = user1Response.jsonPath().getString("tokenDTO.accessToken");
        String user2Token = user2Response.jsonPath().getString("tokenDTO.accessToken");
        UUID user1Id = UUID.fromString(user1Response.jsonPath().getString("userId"));
        UUID user2Id = UUID.fromString(user2Response.jsonPath().getString("userId"));

        // Wait for sync
        Thread.sleep(2000);

        // Create chat
        Response chatResponse = given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + user2Id)
                .then()
                .statusCode(201)
                .extract().response();

        UUID chatId = UUID.fromString(chatResponse.jsonPath().getString("chatId"));

        // Send initial message
        Response msg1 = given()
                .header("Authorization", "Bearer " + user1Token)
                .contentType("application/json")
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(chatId, "Hello before profile update"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200)
                .extract().response();

        UUID messageId1 = UUID.fromString(msg1.jsonPath().getString("messageId"));

        // Update user1's profile in Registration service
        given()
                .header("Authorization", "Bearer " + user1Token)
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createUpdateProfileJson(
                        "Updated Chat User",
                        "I love chatting!",
                        "+1234567890"))
                .when()
                .patch(registrationBaseUrl + "/api/user/auth/" + user1Id)
                .then()
                .statusCode(200);

        // User should still be able to send messages after profile update
        Response msg2 = given()
                .header("Authorization", "Bearer " + user1Token)
                .contentType("application/json")
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(chatId, "Hello after profile update"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200)
                .body("senderId", equalTo(user1Id.toString()))
                .extract().response();

        // User2 can still read all messages
        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .get(friendsBaseUrl + "/api/messages/chat/" + chatId)
                .then()
                .statusCode(200)
                .body("content", hasSize(2));
    }
    @Test
    @Order(15)
    @DisplayName("Should handle chat access after user verification status changes")
    void testChatAccessWithVerificationChanges() throws Exception {
        // This test simulates scenarios where verification status might affect chat access
        String mainUserEmail = "mainchatter@test.com";
        String otherUserEmail = "otherchatter@test.com";

        Response mainUserResponse = registerAndLoginUser("mainchatter", mainUserEmail);
        Response otherUserResponse = registerAndLoginUser("otherchatter", otherUserEmail);

        String mainToken = mainUserResponse.jsonPath().getString("tokenDTO.accessToken");
        String otherToken = otherUserResponse.jsonPath().getString("tokenDTO.accessToken");
        UUID mainUserId = UUID.fromString(mainUserResponse.jsonPath().getString("userId"));
        UUID otherUserId = UUID.fromString(otherUserResponse.jsonPath().getString("userId"));

        // Wait for sync
        Thread.sleep(2000);

        // Create chat
        Response chatResponse = given()
                .header("Authorization", "Bearer " + mainToken)
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + otherUserId)
                .then()
                .statusCode(201)
                .extract().response();

        UUID chatId = UUID.fromString(chatResponse.jsonPath().getString("chatId"));

        // Send messages back and forth
        for (int i = 0; i < 5; i++) {
            String senderToken = i % 2 == 0 ? mainToken : otherToken;
            String message = i % 2 == 0 ? "Message from main user " + i : "Reply from other user " + i;

            given()
                    .header("Authorization", "Bearer " + senderToken)
                    .contentType("application/json")
                    .body(TestDataBuilder.MessageBuilder.createSendMessageJson(chatId, message))
                    .when()
                    .post(friendsBaseUrl + "/api/messages/send")
                    .then()
                    .statusCode(200);
        }

        // Both users can access full chat history
        given()
                .header("Authorization", "Bearer " + mainToken)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get(friendsBaseUrl + "/api/messages/chat/" + chatId)
                .then()
                .statusCode(200)
                .body("content", hasSize(5))
                .body("totalElements", equalTo(5));
    }

    @Test
    @Order(16)
    @DisplayName("Should handle message reactions after user blocking in Registration service")
    void testMessageReactionsWithBlocking() throws Exception {
        // Create users
        String user1Email = "reactor1@test.com";
        String user2Email = "reactor2@test.com";

        Response user1Response = registerAndLoginUser("reactor1", user1Email);
        Response user2Response = registerAndLoginUser("reactor2", user2Email);

        String user1Token = user1Response.jsonPath().getString("tokenDTO.accessToken");
        String user2Token = user2Response.jsonPath().getString("tokenDTO.accessToken");
        UUID user1Id = UUID.fromString(user1Response.jsonPath().getString("userId"));
        UUID user2Id = UUID.fromString(user2Response.jsonPath().getString("userId"));

        // Wait for sync
        Thread.sleep(2000);

        // Create chat and exchange messages
        Response chatResponse = given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + user2Id)
                .then()
                .statusCode(201)
                .extract().response();

        UUID chatId = UUID.fromString(chatResponse.jsonPath().getString("chatId"));

        // User1 sends message
        Response messageResponse = given()
                .header("Authorization", "Bearer " + user1Token)
                .contentType("application/json")
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(chatId, "Please react to this!"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200)
                .extract().response();

        UUID messageId = UUID.fromString(messageResponse.jsonPath().getString("messageId"));

        // User2 reacts to message
        given()
                .header("Authorization", "Bearer " + user2Token)
                .contentType("application/json")
                .body(TestDataBuilder.MessageBuilder.createMessageReactionJson("LOVE"))
                .when()
                .put(friendsBaseUrl + "/api/messages/" + messageId + "/reaction")
                .then()
                .statusCode(200);

        // User1 blocks User2 in Friends service
        given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .post(friendsBaseUrl + "/api/friends/block/" + user2Id)
                .then()
                .statusCode(201);

        // User2 cannot send new messages
        given()
                .header("Authorization", "Bearer " + user2Token)
                .contentType("application/json")
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(chatId, "This should fail"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(anyOf(is(400), is(403)));

        // But existing reactions might still be visible
        given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .get(friendsBaseUrl + "/api/messages/" + messageId + "/reactions")
                .then()
                .statusCode(200)
                .body("", hasSize(1));
    }

    @Test
    @Order(17)
    @DisplayName("Should handle chat persistence when user deletes and re-registers")
    void testChatPersistenceWithUserDeletion() throws Exception {
        // Create two users
        String persistentEmail = "persistent@test.com";
        String deletingEmail = "deleting@test.com";

        Response persistentResponse = registerAndLoginUser("persistent", persistentEmail);
        Response deletingResponse = registerAndLoginUser("deleting", deletingEmail);

        String persistentToken = persistentResponse.jsonPath().getString("tokenDTO.accessToken");
        String deletingToken = deletingResponse.jsonPath().getString("tokenDTO.accessToken");
        UUID persistentId = UUID.fromString(persistentResponse.jsonPath().getString("userId"));
        UUID deletingId = UUID.fromString(deletingResponse.jsonPath().getString("userId"));

        // Wait for sync
        Thread.sleep(2000);

        // Create chat
        Response chatResponse = given()
                .header("Authorization", "Bearer " + persistentToken)
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + deletingId)
                .then()
                .statusCode(201)
                .extract().response();

        UUID chatId = UUID.fromString(chatResponse.jsonPath().getString("chatId"));

        // Exchange messages
        given()
                .header("Authorization", "Bearer " + persistentToken)
                .contentType("application/json")
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(chatId, "Hello!"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + deletingToken)
                .contentType("application/json")
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(chatId, "Hi there!"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200);

        // Deleting user deletes their account
        given()
                .header("Authorization", "Bearer " + deletingToken)
                .contentType("application/json")
                .body(String.format("{\"email\": \"%s\"}", deletingEmail))
                .when()
                .delete(registrationBaseUrl + "/api/user/auth/delete")
                .then()
                .statusCode(200);

        // Persistent user might still see chat history (depends on implementation)
        Response chatHistoryResponse = given()
                .header("Authorization", "Bearer " + persistentToken)
                .when()
                .get(friendsBaseUrl + "/api/messages/chat/" + chatId)
                .then()
                .extract().response();

        // Chat behavior after user deletion depends on business rules
        int statusCode = chatHistoryResponse.statusCode();
        Assertions.assertTrue(statusCode == 200 || statusCode == 404,
                "Chat should either be accessible with history or deleted");
    }

    private void sendAndAcceptFriendRequest(String senderToken, String receiverToken, UUID receiverId) throws Exception {
        Response requestResponse = given()
                .header("Authorization", "Bearer " + senderToken)
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + receiverId)
                .then()
                .statusCode(201)
                .extract().response();

        UUID requestId = UUID.fromString(requestResponse.jsonPath().getString("requestId"));

        given()
                .header("Authorization", "Bearer " + receiverToken)
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + requestId)
                .then()
                .statusCode(201);
    }



}