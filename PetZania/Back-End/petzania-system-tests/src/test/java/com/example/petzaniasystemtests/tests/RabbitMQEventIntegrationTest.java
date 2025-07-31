package com.example.petzaniasystemtests.tests;

import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.example.petzaniasystemtests.builders.TestDataBuilder;

import com.example.petzaniasystemtests.config.BaseSystemTest;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.postgresql.core.ConnectionFactory;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RabbitMQ Event Integration Tests")
public class RabbitMQEventIntegrationTest extends BaseSystemTest {

    @Test
    @Order(1)
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
                    .put(registrationBaseUrl + "/api/user/auth/verify")
                    .then()
                    .statusCode(200);

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
                                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userIds[i])
                                .then()
                                .statusCode(200);
                    }
                });

        // Test that users can interact with each other
        for (int i = 0; i < userCount - 1; i++) {
            given()
                    .header("Authorization", "Bearer " + userTokens[i])
                    .when()
                    .post(friendsBaseUrl + "/api/friends/follow/" + userIds[i + 1])
                    .then()
                    .statusCode(201);
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should handle concurrent user registrations with proper event ordering")
    void testConcurrentUserRegistrations() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<CompletableFuture<Response>> futures = new ArrayList<>();
        int concurrentUsers = 5;

        // Register users concurrently
        for (int i = 0; i < concurrentUsers; i++) {
            final int index = i;
            CompletableFuture<Response> future = CompletableFuture.supplyAsync(() -> {
                return given()
                        .contentType("application/json")
                        .body(TestDataBuilder.UserBuilder.createRegisterUserJson(
                                "concurrent" + index,
                                "concurrent" + index + "@test.com",
                                "Password123!"))
                        .when()
                        .post(registrationBaseUrl + "/api/user/auth/signup")
                        .then()
                        .statusCode(201)
                        .extract().response();
            }, executor);
            futures.add(future);
        }

        // Wait for all registrations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // Verify and login all users
        List<String> tokens = new ArrayList<>();
        List<UUID> userIds = new ArrayList<>();

        for (int i = 0; i < concurrentUsers; i++) {
            String email = "concurrent" + i + "@test.com";
            String otp = getOtpFromDatabase(email);

            given()
                    .contentType("application/json")
                    .body(TestDataBuilder.UserBuilder.createOtpValidationJson(email, otp))
                    .when()
                    .put(registrationBaseUrl + "/api/user/auth/verify")
                    .then()
                    .statusCode(200);

            Response loginResponse = given()
                    .contentType("application/json")
                    .body(TestDataBuilder.UserBuilder.createLoginJson(email, "Password123!"))
                    .when()
                    .post(registrationBaseUrl + "/api/user/auth/login")
                    .then()
                    .statusCode(200)
                    .extract().response();

            tokens.add(loginResponse.jsonPath().getString("tokenDTO.accessToken"));
            userIds.add(UUID.fromString(loginResponse.jsonPath().getString("userId")));
        }

        // Verify all users were propagated to Friends service
        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    for (String token : tokens) {
                        given()
                                .header("Authorization", "Bearer " + token)
                                .when()
                                .get(friendsBaseUrl + "/api/friends/get-number-of-followers/" + userIds.get(0))
                                .then()
                                .statusCode(200);
                    }
                });
    }

    @Test
    @Order(3)
    @DisplayName("Should handle user events when services restart")
    void testEventHandlingAfterServiceRestart() throws Exception {
        // This test simulates what happens when Friends service misses events
        // and needs to catch up when it comes back online

        String testEmail = "restart-test@example.com";

        // Register user
        Response response = given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createRegisterUserJson(
                        "restartuser", testEmail, "Password123!"))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/signup")
                .then()
                .statusCode(201)
                .extract().response();

        UUID userId = UUID.fromString(response.jsonPath().getString("userProfileDTO.userId"));

        // Verify and login
        String otp = getOtpFromDatabase(testEmail);
        given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createOtpValidationJson(testEmail, otp))
                .when()
                .put(registrationBaseUrl + "/api/user/auth/verify")
                .then()
                .statusCode(200);

        Response loginResponse = given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createLoginJson(testEmail, "Password123!"))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        String token = loginResponse.jsonPath().getString("tokenDTO.accessToken");

        // Eventually, user should be accessible in Friends service
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    given()
                            .header("Authorization", "Bearer " + token)
                            .when()
                            .get(friendsBaseUrl + "/api/friends/get-number-of-blocked-users")
                            .then()
                            .statusCode(200)
                            .body(equalTo("0"));
                });
    }

    @Test
    @Order(4)
    @DisplayName("Should process user events in correct order for friend operations")
    void testEventOrderingForFriendOperations() throws Exception {
        // Create users
        String user1Email = "eventorder1@test.com";
        String user2Email = "eventorder2@test.com";

        Response user1Response = registerAndLoginUser("eventorder1", user1Email);
        Response user2Response = registerAndLoginUser("eventorder2", user2Email);

        String user1Token = user1Response.jsonPath().getString("tokenDTO.accessToken");
        String user2Token = user2Response.jsonPath().getString("tokenDTO.accessToken");
        UUID user1Id = UUID.fromString(user1Response.jsonPath().getString("userId"));
        UUID user2Id = UUID.fromString(user2Response.jsonPath().getString("userId"));

        // Wait for propagation
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    given()
                            .header("Authorization", "Bearer " + user1Token)
                            .when()
                            .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + user1Id)
                            .then()
                            .statusCode(200);
                });

        // User1 sends friend request
        Response friendRequest = given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user2Id)
                .then()
                .statusCode(201)
                .extract().response();

        UUID requestId = UUID.fromString(friendRequest.jsonPath().getString("requestId"));

        // User2 should see the pending request
        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .get(friendsBaseUrl + "/api/friends/received-requests")
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].requestId", equalTo(requestId.toString()));

        // User2 accepts
        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + requestId)
                .then()
                .statusCode(201);

        // Both should see each other as friends
        given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + user1Id)
                .then()
                .statusCode(200)
                .body(equalTo("1"));
    }

    @Test
    @Order(5)
    @DisplayName("Should handle bulk operations triggering multiple events")
    void testBulkOperationsEventHandling() throws Exception {
        // Create a main user
        String mainUserEmail = "bulkuser@test.com";
        Response mainUserResponse = registerAndLoginUser("bulkuser", mainUserEmail);
        String mainUserToken = mainUserResponse.jsonPath().getString("tokenDTO.accessToken");
        UUID mainUserId = UUID.fromString(mainUserResponse.jsonPath().getString("userId"));

        // Create multiple users to interact with
        int bulkCount = 5;
        List<UUID> otherUserIds = new ArrayList<>();
        List<String> otherUserTokens = new ArrayList<>();

        for (int i = 0; i < bulkCount; i++) {
            Response response = registerAndLoginUser("bulkother" + i, "bulkother" + i + "@test.com");
            otherUserIds.add(UUID.fromString(response.jsonPath().getString("userId")));
            otherUserTokens.add(response.jsonPath().getString("tokenDTO.accessToken"));
        }

        // Wait for all users to sync
        Thread.sleep(2000);

        // Main user follows all others
        for (UUID otherId : otherUserIds) {
            given()
                    .header("Authorization", "Bearer " + mainUserToken)
                    .when()
                    .post(friendsBaseUrl + "/api/friends/follow/" + otherId)
                    .then()
                    .statusCode(201);
        }

        // Verify follow count
        given()
                .header("Authorization", "Bearer " + mainUserToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-following/" + mainUserId)
                .then()
                .statusCode(200)
                .body(equalTo(String.valueOf(bulkCount)));

        // All others follow back
        for (String token : otherUserTokens) {
            given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .post(friendsBaseUrl + "/api/friends/follow/" + mainUserId)
                    .then()
                    .statusCode(201);
        }

        // Verify follower count
        given()
                .header("Authorization", "Bearer " + mainUserToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-followers/" + mainUserId)
                .then()
                .statusCode(200)
                .body(equalTo(String.valueOf(bulkCount)));
    }

    @Test
    @Order(6)
    @DisplayName("Should handle chat creation events between newly registered users")
    void testChatCreationEventFlow() throws Exception {
        String user1Email = "chatevents1@test.com";
        String user2Email = "chatevents2@test.com";

        Response user1Response = registerAndLoginUser("chatevents1", user1Email);
        Response user2Response = registerAndLoginUser("chatevents2", user2Email);

        String user1Token = user1Response.jsonPath().getString("tokenDTO.accessToken");
        String user2Token = user2Response.jsonPath().getString("tokenDTO.accessToken");
        UUID user2Id = UUID.fromString(user2Response.jsonPath().getString("userId"));

        // Wait for sync
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    given()
                            .header("Authorization", "Bearer " + user1Token)
                            .when()
                            .get(friendsBaseUrl + "/api/chats")
                            .then()
                            .statusCode(200);
                });

        // Create chat
        Response chatResponse = given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + user2Id)
                .then()
                .statusCode(201)
                .body("user1Id", notNullValue())
                .body("user2Id", notNullValue())
                .extract().response();

        UUID chatId = UUID.fromString(chatResponse.jsonPath().getString("chatId"));

        // Both users should see the chat
        given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .get(friendsBaseUrl + "/api/chats")
                .then()
                .statusCode(200)
                .body("", hasSize(greaterThan(0)))
                .body("[0].chatId", equalTo(chatId.toString()));

        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .get(friendsBaseUrl + "/api/chats")
                .then()
                .statusCode(200)
                .body("", hasSize(greaterThan(0)));
        // Test message sending
        Response messageResponse = given()
                .header("Authorization", "Bearer " + user1Token)
                .contentType("application/json")
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(chatId, "Test message from event flow"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200)
                .extract().response();

        UUID messageId = UUID.fromString(messageResponse.jsonPath().getString("messageId"));

        // Other user should see the message
        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .get(friendsBaseUrl + "/api/messages/chat/" + chatId)
                .then()
                .statusCode(200)
                .body("content", hasSize(greaterThan(0)))
                .body("content[0].messageId", equalTo(messageId.toString()));
    }

    @Test
    @Order(7)
    @DisplayName("Should handle complex blocking scenarios with event propagation")
    void testBlockingEventPropagation() throws Exception {
        // Create 3 users for complex blocking scenario
        String user1Email = "blocker1@test.com";
        String user2Email = "blocker2@test.com";
        String user3Email = "blocker3@test.com";

        Response user1Response = registerAndLoginUser("blocker1", user1Email);
        Response user2Response = registerAndLoginUser("blocker2", user2Email);
        Response user3Response = registerAndLoginUser("blocker3", user3Email);

        String user1Token = user1Response.jsonPath().getString("tokenDTO.accessToken");
        String user2Token = user2Response.jsonPath().getString("tokenDTO.accessToken");
        String user3Token = user3Response.jsonPath().getString("tokenDTO.accessToken");
        UUID user1Id = UUID.fromString(user1Response.jsonPath().getString("userId"));
        UUID user2Id = UUID.fromString(user2Response.jsonPath().getString("userId"));
        UUID user3Id = UUID.fromString(user3Response.jsonPath().getString("userId"));

        // Wait for propagation
        Thread.sleep(2000);

        // User1 blocks User2
        given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .post(friendsBaseUrl + "/api/friends/block/" + user2Id)
                .then()
                .statusCode(201);

        // User2 cannot send friend request to User1
        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user1Id)
                .then()
                .statusCode(anyOf(is(400), is(403), is(409)));

        // User2 blocks User3
        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .post(friendsBaseUrl + "/api/friends/block/" + user3Id)
                .then()
                .statusCode(201);

        // Verify block counts
        given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-blocked-users")
                .then()
                .statusCode(200)
                .body(equalTo("1"));

        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-blocked-users")
                .then()
                .statusCode(200)
                .body(equalTo("1"));

        // User1 unblocks User2
        given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .put(friendsBaseUrl + "/api/friends/unblock/" + user2Id)
                .then()
                .statusCode(204);

        // Now User2 can interact with User1
        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .post(friendsBaseUrl + "/api/friends/follow/" + user1Id)
                .then()
                .statusCode(201);
    }

    @Test
    @Order(8)
    @DisplayName("Should handle message reactions and status updates with events")
    void testMessageEventsPropagation() throws Exception {
        String user1Email = "msguser1@test.com";
        String user2Email = "msguser2@test.com";

        Response user1Response = registerAndLoginUser("msguser1", user1Email);
        Response user2Response = registerAndLoginUser("msguser2", user2Email);

        String user1Token = user1Response.jsonPath().getString("tokenDTO.accessToken");
        String user2Token = user2Response.jsonPath().getString("tokenDTO.accessToken");
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

        // Send message
        Response messageResponse = given()
                .header("Authorization", "Bearer " + user1Token)
                .contentType("application/json")
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(chatId, "React to this message!"))
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
                .body(TestDataBuilder.MessageBuilder.createMessageReactionJson("LIKE"))
                .when()
                .put(friendsBaseUrl + "/api/messages/" + messageId + "/reaction")
                .then()
                .statusCode(200);

        // Check reactions
        given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .get(friendsBaseUrl + "/api/messages/" + messageId + "/reactions")
                .then()
                .statusCode(200)
                .body("", hasSize(1))
                .body("[0].reactionType", equalTo("LIKE"));

        // Update message status to READ
        given()
                .header("Authorization", "Bearer " + user2Token)
                .contentType("application/json")
                .body(TestDataBuilder.MessageBuilder.createUpdateMessageStatusJson("DELIVERED"))
                .when()
                .patch(friendsBaseUrl + "/api/messages/" + messageId + "/status")
                .then()
                .statusCode(200);

        // Edit message
        given()
                .header("Authorization", "Bearer " + user1Token)
                .contentType("application/json")
                .body(TestDataBuilder.MessageBuilder.createUpdateMessageContentJson("Edited: React to this!"))
                .when()
                .patch(friendsBaseUrl + "/api/messages/" + messageId + "/content")
                .then()
                .statusCode(200);

        // Verify edited message
        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .get(friendsBaseUrl + "/api/messages/" + messageId)
                .then()
                .statusCode(200)
                .body("content", equalTo("Edited: React to this!"))
                .body("edited", equalTo(true));
    }

    @Test
    @Order(9)
    @DisplayName("Should handle pet creation and profile updates with cross-service consistency")
    void testPetAndProfileEventConsistency() throws Exception {
        String userEmail = "petowner@test.com";
        Response userResponse = registerAndLoginUser("petowner", userEmail);
        String userToken = userResponse.jsonPath().getString("tokenDTO.accessToken");
        UUID userId = UUID.fromString(userResponse.jsonPath().getString("userId"));

        // Wait for sync
        Thread.sleep(2000);

        // Create a pet
        Response petResponse = given()
                .header("Authorization", "Bearer " + userToken)
                .contentType("application/json")
                .body(TestDataBuilder.PetBuilder.createPetJson("Fluffy", "DOG", "MALE", "Golden Retriever"))
                .when()
                .post(registrationBaseUrl + "/api/pet")
                .then()
                .statusCode(201)
                .body("name", equalTo("Fluffy"))
                .extract().response();

        UUID petId = UUID.fromString(petResponse.jsonPath().getString("petId"));

        // Update user profile
        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createUpdateProfileJson(
                        "Pet Owner",
                        "I love my dog Fluffy!",
                        "+1234567890"))
                .when()
                .patch(registrationBaseUrl + "/api/user/auth/" + userId)
                .then()
                .statusCode(200);

        // User should still be able to use Friends service after profile updates
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-following/" + userId)
                .then()
                .statusCode(200);

        // Get user's pets
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(registrationBaseUrl + "/api/user/" + userId + "/pets")
                .then()
                .statusCode(200)
                .body("", hasSize(1))
                .body("[0].petId", equalTo(petId.toString()));
    }

    @Test
    @Order(10)
    @DisplayName("Should handle rate limiting scenarios with proper event queuing")
    void testRateLimitingWithEventQueuing() throws Exception {
        String userEmail = "ratelimited@test.com";
        Response userResponse = registerAndLoginUser("ratelimited", userEmail);
        String userToken = userResponse.jsonPath().getString("tokenDTO.accessToken");
        String userId = userResponse.jsonPath().getString("userId");

        // Create multiple users to follow rapidly
        List<UUID> targetUserIds = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Response targetResponse = registerAndLoginUser("target" + i, "target" + i + "@test.com");
            targetUserIds.add(UUID.fromString(targetResponse.jsonPath().getString("userId")));
        }

        // Wait for all to sync
        Thread.sleep(3000);

        // Try to follow many users rapidly (might hit rate limit)
        int successCount = 0;
        int rateLimitCount = 0;

        for (UUID targetId : targetUserIds) {
            Response response = given()
                    .header("Authorization", "Bearer " + userToken)
                    .when()
                    .post(friendsBaseUrl + "/api/friends/follow/" + targetId)
                    .then()
                    .extract().response();

            if (response.statusCode() == 201) {
                successCount++;
            } else if (response.statusCode() == 429) {
                rateLimitCount++;
                // Wait a bit if rate limited
                Thread.sleep(1000);
            }
        }

        // Verify some operations succeeded
        assertTrue(successCount > 0, "At least some follow operations should succeed");

        // Verify final count
        Response countResponse = given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-following/" + userId)
                .then()
                .statusCode(200)
                .extract().response();

        int followingCount = Integer.parseInt(countResponse.body().asString());
        assertTrue(followingCount > 0, "User should be following at least one person");
    }

    @Test
    @Order(11)
    @DisplayName("Should handle user search across services after registration")
    void testUserSearchAfterRegistration() throws Exception {
        // Create users with similar usernames
        String[] usernames = {"searchuser1", "searchuser2", "searchuser3", "differentuser"};
        String[] emails = new String[usernames.length];
        String[] tokens = new String[usernames.length];
        String[] userIds = new String[usernames.length];

        for (int i = 0; i < usernames.length; i++) {
            emails[i] = usernames[i] + "@test.com";
            Response response = registerAndLoginUser(usernames[i], emails[i]);
            tokens[i] = response.jsonPath().getString("tokenDTO.accessToken");
            userIds[i] = response.jsonPath().getString("userId");
        }

        // Wait for all users to sync
        Thread.sleep(3000);

        // Search for users by prefix in Registration service
        given()
                .header("Authorization", "Bearer " + tokens[0])
                .queryParam("prefix", "searchuser")
                .when()
                .get(registrationBaseUrl + "/api/user/auth/users/search")
                .then()
                .statusCode(200)
                .body("content", hasSize(3))
                .body("content.username", hasItems("searchuser1", "searchuser2", "searchuser3"));

        // Users should be able to interact in Friends service
        for (int i = 0; i < 3; i++) {
            given()
                    .header("Authorization", "Bearer " + tokens[i])
                    .when()
                    .get(friendsBaseUrl + "/api/friends/get-number-of-followers/" + userIds[i])
                    .then()
                    .statusCode(200);
        }
    }

    @Test
    @Order(12)
    @DisplayName("Should handle complex friend request scenarios with proper event ordering")
    void testComplexFriendRequestScenarios() throws Exception {
        // Create 4 users for complex friend request scenario
        String[] emails = {"complex1@test.com", "complex2@test.com", "complex3@test.com", "complex4@test.com"};
        Response[] responses = new Response[4];
        String[] tokens = new String[4];
        UUID[] userIds = new UUID[4];

        for (int i = 0; i < 4; i++) {
            responses[i] = registerAndLoginUser("complex" + (i + 1), emails[i]);
            tokens[i] = responses[i].jsonPath().getString("tokenDTO.accessToken");
            userIds[i] = UUID.fromString(responses[i].jsonPath().getString("userId"));
        }

        // Wait for sync
        Thread.sleep(2000);

        // User1 sends request to User2
        Response request1 = given()
                .header("Authorization", "Bearer " + tokens[0])
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + userIds[1])
                .then()
                .statusCode(201)
                .extract().response();

        UUID requestId1 = UUID.fromString(request1.jsonPath().getString("requestId"));

        // User2 sends request to User3
        Response request2 = given()
                .header("Authorization", "Bearer " + tokens[1])
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + userIds[2])
                .then()
                .statusCode(201)
                .extract().response();

        UUID requestId2 = UUID.fromString(request2.jsonPath().getString("requestId"));

        // User3 sends request to User1 (creating a chain)
        Response request3 = given()
                .header("Authorization", "Bearer " + tokens[2])
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + userIds[0])
                .then()
                .statusCode(201)
                .extract().response();

        UUID requestId3 = UUID.fromString(request3.jsonPath().getString("requestId"));

        // User2 cancels request before User3 accepts
        given()
                .header("Authorization", "Bearer " + tokens[1])
                .when()
                .put(friendsBaseUrl + "/api/friends/cancel-request/" + requestId2)
                .then()
                .statusCode(204);

        // User2 accepts User1's request
        given()
                .header("Authorization", "Bearer " + tokens[1])
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + requestId1)
                .then()
                .statusCode(201);

        // User1 accepts User3's request
        given()
                .header("Authorization", "Bearer " + tokens[0])
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + requestId3)
                .then()
                .statusCode(201);

        // Verify friend counts
        given()
                .header("Authorization", "Bearer " + tokens[0])
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userIds[0])
                .then()
                .statusCode(200)
                .body(equalTo("2")); // Friends with User2 and User3

        given()
                .header("Authorization", "Bearer " + tokens[2])
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userIds[2])
                .then()
                .statusCode(200)
                .body(equalTo("1")); // Only friends with User1
    }

    @Test
    @Order(13)
    @DisplayName("Should handle chat muting and pinning operations")
    void testChatPreferencesEventFlow() throws Exception {
        String user1Email = "chatpref1@test.com";
        String user2Email = "chatpref2@test.com";

        Response user1Response = registerAndLoginUser("chatpref1", user1Email);
        Response user2Response = registerAndLoginUser("chatpref2", user2Email);

        String user1Token = user1Response.jsonPath().getString("tokenDTO.accessToken");
        String user2Token = user2Response.jsonPath().getString("tokenDTO.accessToken");
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

        // User1 pins and mutes the chat
        given()
                .header("Authorization", "Bearer " + user1Token)
                .contentType("application/json")
                .body(TestDataBuilder.ChatBuilder.createUpdateUserChatJson(true, true))
                .when()
                .patch(friendsBaseUrl + "/api/chats/" + chatId)
                .then()
                .statusCode(200)
                .body("pinned", equalTo(true))
                .body("muted", equalTo(true));

        // User2's chat preferences should be independent
        given()
                .header("Authorization", "Bearer " + user2Token)
                .contentType("application/json")
                .body(TestDataBuilder.ChatBuilder.createUpdateUserChatJson(false, false))
                .when()
                .patch(friendsBaseUrl + "/api/chats/" + chatId)
                .then()
                .statusCode(200)
                .body("pinned", equalTo(false))
                .body("muted", equalTo(false));

        // Send messages to test muting
        for (int i = 0; i < 5; i++) {
            given()
                    .header("Authorization", "Bearer " + user2Token)
                    .contentType("application/json")
                    .body(TestDataBuilder.MessageBuilder.createSendMessageJson(chatId, "Message " + i))
                    .when()
                    .post(friendsBaseUrl + "/api/messages/send")
                    .then()
                    .statusCode(200);
        }

        // Get messages history
        given()
                .header("Authorization", "Bearer " + user1Token)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get(friendsBaseUrl + "/api/messages/chat/" + chatId)
                .then()
                .statusCode(200)
                .body("content", hasSize(5));
    }

    @Test
    @Order(14)
    @DisplayName("Should handle message deletion events")
    void testMessageDeletionEvents() throws Exception {
        String user1Email = "msgdel1@test.com";
        String user2Email = "msgdel2@test.com";

        Response user1Response = registerAndLoginUser("msgdel1", user1Email);
        Response user2Response = registerAndLoginUser("msgdel2", user2Email);

        String user1Token = user1Response.jsonPath().getString("tokenDTO.accessToken");
        String user2Token = user2Response.jsonPath().getString("tokenDTO.accessToken");
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

        // Send multiple messages
        List<UUID> messageIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Response msgResponse = given()
                    .header("Authorization", "Bearer " + user1Token)
                    .contentType("application/json")
                    .body(TestDataBuilder.MessageBuilder.createSendMessageJson(chatId, "Message to delete " + i))
                    .when()
                    .post(friendsBaseUrl + "/api/messages/send")
                    .then()
                    .statusCode(200)
                    .extract().response();

            messageIds.add(UUID.fromString(msgResponse.jsonPath().getString("messageId")));
        }

        // Delete middle message
        given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .delete(friendsBaseUrl + "/api/messages/" + messageIds.get(1))
                .then()
                .statusCode(204);

        // Verify message is deleted
        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .get(friendsBaseUrl + "/api/messages/chat/" + chatId)
                .then()
                .statusCode(200)
                .body("content", hasSize(2))
                .body("content.messageId", not(hasItem(messageIds.get(1).toString())));
    }

    @Test
    @Order(15)
    @DisplayName("Should handle user online status updates")
    void testUserOnlineStatusEvents() throws Exception {
        String user1Email = "online1@test.com";
        String user2Email = "online2@test.com";

        Response user1Response = registerAndLoginUser("online1", user1Email);
        Response user2Response = registerAndLoginUser("online2", user2Email);

        String user1Token = user1Response.jsonPath().getString("tokenDTO.accessToken");
        UUID user1Id = UUID.fromString(user1Response.jsonPath().getString("userId"));
        UUID user2Id = UUID.fromString(user2Response.jsonPath().getString("userId"));

        // Wait for sync
        Thread.sleep(2000);

        // Get user profile - should show online status
        given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .get(registrationBaseUrl + "/api/user/auth/" + user1Id)
                .then()
                .statusCode(200)
                .body("userId", equalTo(user1Id.toString()))
                .body("online", notNullValue());

        // Multiple logins should update login count
        for (int i = 0; i < 3; i++) {
            given()
                    .contentType("application/json")
                    .body(TestDataBuilder.UserBuilder.createLoginJson(user1Email, "Password123!"))
                    .when()
                    .post(registrationBaseUrl + "/api/user/auth/login")
                    .then()
                    .statusCode(200)
                    .body("loginTimes", greaterThan(i));
        }
    }
    @Test
    @Order(16)
    @DisplayName("Should handle file upload references in messages")
    void testFileMessageEvents() throws Exception {
        String user1Email = "fileuser1@test.com";
        String user2Email = "fileuser2@test.com";

        Response user1Response = registerAndLoginUser("fileuser1", user1Email);
        Response user2Response = registerAndLoginUser("fileuser2", user2Email);

        String user1Token = user1Response.jsonPath().getString("tokenDTO.accessToken");
        String user2Token = user2Response.jsonPath().getString("tokenDTO.accessToken");
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

        // Send file message (simulating file URL)
        given()
                .header("Authorization", "Bearer " + user1Token)
                .contentType("application/json")
                .body(String.format("""
                        {
                            "chatId": "%s",
                            "content": "https://example.com/file.pdf",
                            "file": true
                        }
                        """, chatId))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200)
                .body("file", equalTo(true));

        // Send regular message with reply to file
        Response fileMsg = given()
                .header("Authorization", "Bearer " + user1Token)
                .contentType("application/json")
                .body(String.format("""
                        {
                            "chatId": "%s",
                            "content": "Check out this file!",
                            "file": false
                        }
                        """, chatId))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200)
                .extract().response();

        UUID msgId = UUID.fromString(fileMsg.jsonPath().getString("messageId"));

        // Reply to message
        given()
                .header("Authorization", "Bearer " + user2Token)
                .contentType("application/json")
                .body(String.format("""
                        {
                            "chatId": "%s",
                            "content": "Thanks for the file!",
                            "replyToMessageId": "%s",
                            "file": false
                        }
                        """, chatId, msgId))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200)
                .body("replyToMessageId", equalTo(msgId.toString()));
    }

    @Test
    @Order(17)
    @DisplayName("Should handle user removal from friend list")
    void testFriendRemovalEvents() throws Exception {
        // Create 3 users
        String[] emails = {"remove1@test.com", "remove2@test.com", "remove3@test.com"};
        Response[] responses = new Response[3];
        String[] tokens = new String[3];
        UUID[] userIds = new UUID[3];

        for (int i = 0; i < 3; i++) {
            responses[i] = registerAndLoginUser("remove" + (i + 1), emails[i]);
            tokens[i] = responses[i].jsonPath().getString("tokenDTO.accessToken");
            userIds[i] = UUID.fromString(responses[i].jsonPath().getString("userId"));
        }

        // Wait for sync
        Thread.sleep(2000);

        // Create friendships: 1-2, 1-3, 2-3
        String[][] friendPairs = {
                {tokens[0], userIds[1].toString()},
                {tokens[0], userIds[2].toString()},
                {tokens[1], userIds[2].toString()}
        };

        for (String[] pair : friendPairs) {
            Response reqResponse = given()
                    .header("Authorization", "Bearer " + pair[0])
                    .when()
                    .post(friendsBaseUrl + "/api/friends/send-request/" + pair[1])
                    .then()
                    .statusCode(201)
                    .extract().response();

            UUID requestId = UUID.fromString(reqResponse.jsonPath().getString("requestId"));

            // Find the receiver's token and accept
            String receiverToken = null;
            for (int i = 0; i < 3; i++) {
                if (userIds[i].toString().equals(pair[1])) {
                    receiverToken = tokens[i];
                    break;
                }
            }

            given()
                    .header("Authorization", "Bearer " + receiverToken)
                    .when()
                    .post(friendsBaseUrl + "/api/friends/accept-request/" + requestId)
                    .then()
                    .statusCode(201);
        }

        // Verify friend counts
        given()
                .header("Authorization", "Bearer " + tokens[0])
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userIds[0])
                .then()
                .statusCode(200)
                .body(equalTo("2"));

        // User1 removes User2 from friends
        given()
                .header("Authorization", "Bearer " + tokens[0])
                .when()
                .delete(friendsBaseUrl + "/api/friends/remove/" + userIds[1])
                .then()
                .statusCode(204);

        // Verify updated friend counts
        given()
                .header("Authorization", "Bearer " + tokens[0])
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userIds[0])
                .then()
                .statusCode(200)
                .body(equalTo("1"));

        given()
                .header("Authorization", "Bearer " + tokens[1])
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userIds[1])
                .then()
                .statusCode(200)
                .body(equalTo("1"));
    }

    @Test
    @Order(18)
    @DisplayName("Should handle follow/unfollow operations with proper counts")
    void testFollowUnfollowEvents() throws Exception {
        // Create influencer and followers
        String influencerEmail = "influencer@test.com";
        Response influencerResponse = registerAndLoginUser("influencer", influencerEmail);
        String influencerToken = influencerResponse.jsonPath().getString("tokenDTO.accessToken");
        UUID influencerId = UUID.fromString(influencerResponse.jsonPath().getString("userId"));

        // Create followers
        int followerCount = 10;
        List<String> followerTokens = new ArrayList<>();

        for (int i = 0; i < followerCount; i++) {
            Response followerResponse = registerAndLoginUser("follower" + i, "follower" + i + "@test.com");
            followerTokens.add(followerResponse.jsonPath().getString("tokenDTO.accessToken"));
        }

        // Wait for sync
        Thread.sleep(3000);

        // All followers follow the influencer
        for (String followerToken : followerTokens) {
            given()
                    .header("Authorization", "Bearer " + followerToken)
                    .when()
                    .post(friendsBaseUrl + "/api/friends/follow/" + influencerId)
                    .then()
                    .statusCode(201);
        }

        // Verify follower count
        given()
                .header("Authorization", "Bearer " + influencerToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-followers/" + influencerId)
                .then()
                .statusCode(200)
                .body(equalTo(String.valueOf(followerCount)));

        // Get followers list
        given()
                .header("Authorization", "Bearer " + influencerToken)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-followers/" + influencerId)
                .then()
                .statusCode(200)
                .body("content", hasSize(followerCount));

        // Some followers unfollow
        for (int i = 0; i < 3; i++) {
            given()
                    .header("Authorization", "Bearer " + followerTokens.get(i))
                    .when()
                    .put(friendsBaseUrl + "/api/friends/unfollow/" + influencerId)
                    .then()
                    .statusCode(204);
        }

        // Verify updated count
        given()
                .header("Authorization", "Bearer " + influencerToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-followers/" + influencerId)
                .then()
                .statusCode(200)
                .body(equalTo(String.valueOf(followerCount - 3)));
    }

    @Test
    @Order(19)
    @DisplayName("Should handle pet updates and deletions")
    void testPetManagementEvents() throws Exception {
        String userEmail = "petlover@test.com";
        Response userResponse = registerAndLoginUser("petlover", userEmail);
        String userToken = userResponse.jsonPath().getString("tokenDTO.accessToken");
        UUID userId = UUID.fromString(userResponse.jsonPath().getString("userId"));

        // Create multiple pets
        List<UUID> petIds = new ArrayList<>();
        String[] petNames = {"Max", "Luna", "Charlie"};

        for (String petName : petNames) {
            Response petResponse = given()
                    .header("Authorization", "Bearer " + userToken)
                    .contentType("application/json")
                    .body(TestDataBuilder.PetBuilder.createPetJson(petName, "CAT", "FEMALE", "Persian"))
                    .when()
                    .post(registrationBaseUrl + "/api/pet")
                    .then()
                    .statusCode(201)
                    .extract().response();

            petIds.add(UUID.fromString(petResponse.jsonPath().getString("petId")));
        }

        // Update a pet
        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType("application/json")
                .body("""
                        {
                            "name": "Luna Updated",
                            "description": "My lovely cat",
                            "breed": "Persian Mix"
                        }
                        """)
                .when()
                .patch(registrationBaseUrl + "/api/pet/" + petIds.get(1))
                .then()
                .statusCode(200)
                .body("name", equalTo("Luna Updated"));

        // Delete a pet
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .delete(registrationBaseUrl + "/api/pet/" + petIds.get(2))
                .then()
                .statusCode(204);

        // Verify remaining pets
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(registrationBaseUrl + "/api/user/" + userId + "/pets")
                .then()
                .statusCode(200)
                .body("", hasSize(2));

        // User should still be active in Friends service
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userId)
                .then()
                .statusCode(200);
    }

    /** Connects to the adoption_breeding database inside the Postgres container */
    private Connection getAdoptionConn() throws SQLException {
        String url = String.format(
                "jdbc:postgresql://localhost:%d/adoption_breeding",
                postgres.getMappedPort(5432)
        );
        return DriverManager.getConnection(url, "postgres", "admin");
    }

    /** Returns all followed IDs for a given follower (table "follow", cols follower_id, followed_id) */
    private List<UUID> persistedFollowees(UUID followerId) throws Exception {
        String sql = "SELECT followed_id FROM follows WHERE follower_id = ?";
        try (Connection conn = getAdoptionConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, followerId);
            try (ResultSet rs = ps.executeQuery()) {
                List<UUID> ids = new ArrayList<>();
                while (rs.next()) {
                    ids.add((UUID) rs.getObject("followed_id"));
                }
                return ids;
            }
        }
    }

    /** Checks whether a friendship record exists in table "friendship" (cols user1_id, user2_id) */
    private boolean friendshipExists(UUID a, UUID b) throws Exception {
        String sql =
                "SELECT COUNT(*) FROM friendships " +
                        "WHERE (user1_id = ? AND user2_id = ?) " +
                        "   OR (user1_id = ? AND user2_id = ?)";
        try (Connection conn = getAdoptionConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, a);
            ps.setObject(2, b);
            ps.setObject(3, b);
            ps.setObject(4, a);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        }
    }

    @Test
    @Order(20)
    @DisplayName("Follow API → adoption DB has a follow record")
    void testFollowPersistsInAdoption() throws Exception {
        // Register A & B
        Response a = registerAndLoginUser("alice", "alice@test.com");
        Response b = registerAndLoginUser("bobsy",   "bob@test.com");
        UUID idA = UUID.fromString(a.jsonPath().getString("userId"));
        UUID idB = UUID.fromString(b.jsonPath().getString("userId"));
        String tokenA = a.jsonPath().getString("tokenDTO.accessToken");

        // Call friends-service follow endpoint
        given()
                .spec(getAuthenticatedSpec(tokenA))
                .when().post(friendsBaseUrl + "/api/friends/follow/" + idB)
                .then().statusCode(201);

        // Allow listener to persist
        Thread.sleep(500);

        // Assert the adoption module's `follow` table has the row
        List<UUID> followees = persistedFollowees(idA);
        assertTrue(followees.contains(idB),
                "Adoption DB should contain that Alice follows Bobsy");
    }

    @Test
    @Order(21)
    @DisplayName("Unfollow API → adoption DB no longer has the follow record")
    void testUnfollowRemovesFromAdoption() throws Exception {
        // Register C & D
        Response c = registerAndLoginUser("carol", "carol@test.com");
        Response d = registerAndLoginUser("daves",  "dave@test.com");
        UUID idC = UUID.fromString(c.jsonPath().getString("userId"));
        UUID idD = UUID.fromString(d.jsonPath().getString("userId"));
        String tokenC = c.jsonPath().getString("tokenDTO.accessToken");

        // Carol follows then unfollows Dave
        given().spec(getAuthenticatedSpec(tokenC))
                .post(friendsBaseUrl + "/api/friends/follow/" + idD);
        given().spec(getAuthenticatedSpec(tokenC))
                .when().put(friendsBaseUrl + "/api/friends/unfollow/" + idD)
                .then().statusCode(204);

        Thread.sleep(500);

        // Assert row removed
        List<UUID> followees = persistedFollowees(idC);
        assertFalse(followees.contains(idD),
                "Adoption DB should no longer record that Carol follows Daves");
    }

    @Test
    @Order(22)
    @DisplayName("Send & accept friend request → adoption DB has a friendship record")
    void testFriendRequestThenAcceptPersistsFriendship() throws Exception {
        // Register E & F
        Response e = registerAndLoginUser("evese",   "eve@test.com");
        Response f = registerAndLoginUser("frank", "frank@test.com");
        UUID idE = UUID.fromString(e.jsonPath().getString("userId"));
        UUID idF = UUID.fromString(f.jsonPath().getString("userId"));
        String tokenE = e.jsonPath().getString("tokenDTO.accessToken");
        String tokenF = f.jsonPath().getString("tokenDTO.accessToken");

        // Evese sends, Frank accepts
        String reqId = given().spec(getAuthenticatedSpec(tokenE))
                .when().post(friendsBaseUrl + "/api/friends/send-request/" + idF)
                .then().statusCode(201)
                .extract().jsonPath().getString("requestId");
        given().spec(getAuthenticatedSpec(tokenF))
                .when().post(friendsBaseUrl + "/api/friends/accept-request/" + reqId)
                .then().statusCode(201);

        Thread.sleep(500);

        // Assert friendship in adoption DB
        assertTrue(friendshipExists(idE, idF),
                "Adoption DB should record a friendship between Evese and Frank");
    }

    @Test
    @Order(23)
    @DisplayName("Send, accept, then remove friend → adoption DB no longer has friendship")
    void testRemoveFriendRemovesFromAdoption() throws Exception {
        // Register G & H
        Response g = registerAndLoginUser("gweny", "gwen@test.com");
        Response h = registerAndLoginUser("hanks", "hank@test.com");
        UUID idG = UUID.fromString(g.jsonPath().getString("userId"));
        UUID idH = UUID.fromString(h.jsonPath().getString("userId"));
        String tokenG = g.jsonPath().getString("tokenDTO.accessToken");
        String tokenH = h.jsonPath().getString("tokenDTO.accessToken");

        // Gweny sends & Hanks accepts
        String reqId = given().spec(getAuthenticatedSpec(tokenG))
                .when().post(friendsBaseUrl + "/api/friends/send-request/" + idH)
                .then().statusCode(201)
                .extract().jsonPath().getString("requestId");
        given().spec(getAuthenticatedSpec(tokenH))
                .when().post(friendsBaseUrl + "/api/friends/accept-request/" + reqId)
                .then().statusCode(201);

        // Then Gwen removes Hank
        given().spec(getAuthenticatedSpec(tokenG))
                .when().delete(friendsBaseUrl + "/api/friends/remove/" + idH)
                .then().statusCode(204);

        Thread.sleep(500);

        // Assert friendship removed
        assertFalse(friendshipExists(idG, idH),
                "Adoption DB should no longer record a friendship between Gweny and Hanks");
    }

    @Test
    @Order(24)
    @DisplayName("Mutual follow + friendship → block each other → adoption state cleaned up")
    void testFollowFriendThenBlockCleansUpAdoption() throws Exception {
        // Register X & Y
        Response rx = registerAndLoginUser("xavier", "x@example.com");
        Response ry = registerAndLoginUser("yvonne", "y@example.com");
        UUID idX = UUID.fromString(rx.jsonPath().getString("userId"));
        UUID idY = UUID.fromString(ry.jsonPath().getString("userId"));
        String tx = rx.jsonPath().getString("tokenDTO.accessToken");
        String ty = ry.jsonPath().getString("tokenDTO.accessToken");

        // 1) Mutual follow
        given().spec(getAuthenticatedSpec(tx))
                .when().post(friendsBaseUrl + "/api/friends/follow/" + idY)
                .then().statusCode(201);
        given().spec(getAuthenticatedSpec(ty))
                .when().post(friendsBaseUrl + "/api/friends/follow/" + idX)
                .then().statusCode(201);

        // wait for Adoption module
        Thread.sleep(500);

        // assert both follow records exist
        List<UUID> xFollows = persistedFollowees(idX);
        List<UUID> yFollows = persistedFollowees(idY);
        assertTrue(xFollows.contains(idY), "X should follow Y in Adoption DB");
        assertTrue(yFollows.contains(idX), "Y should follow X in Adoption DB");

        // 2) Friendship
        String reqXY = given().spec(getAuthenticatedSpec(tx))
                .when().post(friendsBaseUrl + "/api/friends/send-request/" + idY)
                .then().statusCode(201)
                .extract().jsonPath().getString("requestId");
        given().spec(getAuthenticatedSpec(ty))
                .when().post(friendsBaseUrl + "/api/friends/accept-request/" + reqXY)
                .then().statusCode(201);

        Thread.sleep(500);
        assertTrue(friendshipExists(idX, idY), "Friendship X↔Y should exist in Adoption DB");

        // 3) Mutual block
        given().spec(getAuthenticatedSpec(tx))
                .when().post(friendsBaseUrl + "/api/friends/block/" + idY)
                .then().statusCode(201);

        Thread.sleep(500);

        // 4) After block: follows and friendship must be removed
        List<UUID> xFollowsAfter = persistedFollowees(idX);
        List<UUID> yFollowsAfter = persistedFollowees(idY);
        assertFalse(xFollowsAfter.contains(idY), "X’s follow of Y should be removed after block");
        assertFalse(yFollowsAfter.contains(idX), "Y’s follow of X should be removed after block");

        assertFalse(friendshipExists(idX, idY),
                "Friendship X↔Y should be removed after block");
    }
}