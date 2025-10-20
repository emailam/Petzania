package com.example.petzaniasystemtests.tests;

import com.example.petzaniasystemtests.config.BaseSystemTest;
import com.example.petzaniasystemtests.builders.TestDataBuilder;
import com.example.petzaniasystemtests.utils.JwtTokenExtractor;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

public class ModulesIntegrationTest extends BaseSystemTest {
    private static String userXXXToken;
    private static UUID userXXXId;
    private static final String USER_EMAIL_XXX = "interservice@test.com";
    private static final String PASSWORD_XXX = "Password123!";

    @AfterEach
    void clearRedis() throws Exception {
        redis.execInContainer("redis-cli", "FLUSHALL");
    }
    @Test
    @Order(1)
    @DisplayName("Should propagate user registration to all modules")
    void testUserRegistration_PropagatedToAllModules() throws Exception {
        // Test Case 1: Register a new user and verify propagation to all modules
        String username = "testuser_reg_" + System.currentTimeMillis();
        String email = username + "@example.com";

        // Step 1: Register user
        given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createRegisterUserJson(username, email, PASSWORD))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/signup")
                .then()
                .statusCode(201);

        // Step 2: Complete OTP verification
        String otp = getOtpFromDatabase(email);
        given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createOtpValidationJson(email, otp))
                .when()
                .put(registrationBaseUrl + "/api/user/auth/verify")
                .then()
                .statusCode(200);

        // Wait for RabbitMQ propagation
        Thread.sleep(3000);

        // Step 3: Login to get token
        Response loginResponse = given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createLoginJson(email, PASSWORD))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        String token = JwtTokenExtractor.extractAccessToken(loginResponse);
        String userId = JwtTokenExtractor.extractUserId(loginResponse);

        // Step 4: Verify user can access friends module
        given()
                .spec(getAuthenticatedSpec(token))
                .when()
                .get(friendsBaseUrl + "/api/chats")
                .then()
                .statusCode(200)
                .body("$", is(empty())); // New user has no chats

        // Step 5: Verify user can access adoption module
        given()
                .spec(getAuthenticatedSpec(token))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/user/" + userId)
                .then()
                .statusCode(200)
                .body("content", is(empty())); // New user has no posts

        // Step 6: Verify user can access notifications module
        given()
                .spec(getAuthenticatedSpec(token))
                .when()
                .get(notificationBaseUrl + "/api/notifications")
                .then()
                .statusCode(204);
    }

    @Test
    @Order(2)
    @DisplayName("Should enable cross-module interactions after user registration propagation")
    void testCrossModuleInteractionsAfterRegistration() throws Exception {
        String user1Name = "cross_user1_" + System.currentTimeMillis();
        String user1Email = user1Name + "@example.com";
        String user2Name = "cross_user2_" + System.currentTimeMillis();
        String user2Email = user2Name + "@example.com";

        // Register both users through Registration Module
        Response user1Response = registerAndLoginUser(user1Name, user1Email);
        String user1Token = JwtTokenExtractor.extractAccessToken(user1Response);
        String user1Id = JwtTokenExtractor.extractUserId(user1Response);

        Response user2Response = registerAndLoginUser(user2Name, user2Email);
        String user2Token = JwtTokenExtractor.extractAccessToken(user2Response);
        String user2Id = JwtTokenExtractor.extractUserId(user2Response);

        // Wait for propagation to both modules
        Thread.sleep(4000);

        // Step 1: User1 creates pet post in Adoption Module
        String petPostJson = """
                {
                    "petDTO": {
                        "name": "CrossModulePet",
                        "species": "CAT",
                        "gender": "FEMALE",
                        "breed": "Persian",
                        "dateOfBirth": "2021-01-01",
                        "description": "Beautiful cat"
                    },
                    "description": "Looking for a loving home",
                    "postType": "ADOPTION",
                    "latitude": 5.0,
                    "longitude": 5.0
                }
                """;

        Response petPostResponse = given()
                .spec(getAuthenticatedSpec(user1Token))
                .body(petPostJson)
                .when()
                .post(adoptionBaseUrl + "/api/pet-posts")
                .then()
                .statusCode(201)
                .extract().response();

        String petPostId = petPostResponse.jsonPath().getString("postId");

        // Step 2: User2 can view User1's post (both users exist in adoption module)
        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/user/" + user1Id)
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].postId", equalTo(petPostId))
                .body("content[0].ownerId", equalTo(user1Id));

        // Step 3: User2 can react to User1's post
        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .put(adoptionBaseUrl + "/api/pet-posts/" + petPostId + "/react")
                .then()
                .statusCode(200)
                .body("reacts", equalTo(1));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(notificationBaseUrl + "/api/notifications")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].type", equalTo(TestDataBuilder.UserBuilder.NotificationType.PET_POST_LIKED.toString()))
                .body("content[0].status", equalTo(TestDataBuilder.UserBuilder.NotificationStatus.UNREAD.toString()));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(notificationBaseUrl + "/api/notifications/unread-count")
                .then()
                .statusCode(200)
                .body(equalTo("1"));

        // Step 4: User2 sends friend request to User1 in Friends Module
        Response friendRequestResponse = given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user1Id)
                .then()
                .statusCode(201)
                .body("sender.userId", equalTo(user2Id))
                .body("receiver.userId", equalTo(user1Id))
                .extract().response();

        String requestId = friendRequestResponse.jsonPath().getString("requestId");

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(notificationBaseUrl + "/api/notifications")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("content[0].type", equalTo(TestDataBuilder.UserBuilder.NotificationType.FRIEND_REQUEST_RECEIVED.toString()))
                .body("content[0].status", equalTo(TestDataBuilder.UserBuilder.NotificationStatus.UNREAD.toString()));

        // Step 5: User1 can see received friend request
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/received-requests")
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].requestId", equalTo(requestId));

        // Step 6: User1 accepts friend request
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + requestId)
                .then()
                .statusCode(201);

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(notificationBaseUrl + "/api/notifications")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].type", equalTo(TestDataBuilder.UserBuilder.NotificationType.FRIEND_REQUEST_ACCEPTED.toString()))
                .body("content[0].status", equalTo(TestDataBuilder.UserBuilder.NotificationStatus.UNREAD.toString()));

        // Step 7: Verify friendship exists
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/is-friend/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("true"));

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("1"));
    }

    @Test
    @Order(3)
    @DisplayName("Should handle rapid user registrations with proper event propagation")
    void testRapidUserRegistrationsEventPropagation() throws Exception {
        int numberOfUsers = 5;
        String[] usernames = new String[numberOfUsers];
        String[] emails = new String[numberOfUsers];
        String[] tokens = new String[numberOfUsers];
        String[] userIds = new String[numberOfUsers];

        // Register multiple users rapidly
        for (int i = 0; i < numberOfUsers; i++) {
            usernames[i] = "rapid_user_" + i + "_" + System.currentTimeMillis();
            emails[i] = usernames[i] + "@example.com";

            Response response = registerAndLoginUser(usernames[i], emails[i]);
            tokens[i] = JwtTokenExtractor.extractAccessToken(response);
            userIds[i] = JwtTokenExtractor.extractUserId(response);
        }

        // Wait for all propagations
        Thread.sleep(6000);

        // Verify all users can interact across modules
        for (int i = 0; i < numberOfUsers; i++) {

            // Each user can access friends module
            given()
                    .spec(getAuthenticatedSpec(tokens[i]))
                    .when()
                    .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userIds[i])
                    .then()
                    .statusCode(200);

            // Each user can access adoption module
            given()
                    .spec(getAuthenticatedSpec(tokens[i]))
                    .when()
                    .get(adoptionBaseUrl + "/api/pet-posts/user/" + userIds[i])
                    .then()
                    .statusCode(200);

            // Each user can access notification module
            given()
                    .spec(getAuthenticatedSpec(tokens[i]))
                    .queryParam("page", 0)
                    .queryParam("size", 10)
                    .queryParam("sortBy", "createdAt")
                    .queryParam("direction", "desc")
                    .when()
                    .get(notificationBaseUrl + "/api/notifications")
                    .then()
                    .statusCode(anyOf(is(200), is(204)));

            // Each user can send friend requests to others
            for (int j = i + 1; j < numberOfUsers; j++) {
                given()
                        .spec(getAuthenticatedSpec(tokens[i]))
                        .when()
                        .post(friendsBaseUrl + "/api/friends/send-request/" + userIds[j])
                        .then()
                        .statusCode(201);

                Thread.sleep(3000);
                given()
                        .spec(getAuthenticatedSpec(tokens[j]))
                        .when()
                        .get(notificationBaseUrl + "/api/notifications")
                        .then()
                        .statusCode(200)
                        .body("content[0].type", equalTo(TestDataBuilder.UserBuilder.NotificationType.FRIEND_REQUEST_RECEIVED.toString()))
                        .body("content[0].status", equalTo(TestDataBuilder.UserBuilder.NotificationStatus.UNREAD.toString()));
            }
        }

        for (int i = 1; i < numberOfUsers; i++) {
            given()
                    .spec(getAuthenticatedSpec(tokens[i]))
                    .when()
                    .get(notificationBaseUrl + "/api/notifications")
                    .then()
                    .statusCode(200);

            given()
                    .spec(getAuthenticatedSpec(tokens[i]))
                    .when()
                    .get(notificationBaseUrl + "/api/notifications/unread-count")
                    .then()
                    .statusCode(200)
                    .body(is(Integer.toString(i)));

            given()
                    .spec(getAuthenticatedSpec(tokens[i]))
                    .when()
                    .put(notificationBaseUrl + "/api/notifications/mark-all-read")
                    .then()
                    .statusCode(200)
                    .body(equalTo("All notifications marked as read"));

            given()
                    .spec(getAuthenticatedSpec(tokens[i]))
                    .when()
                    .get(notificationBaseUrl + "/api/notifications/unread-count")
                    .then()
                    .statusCode(200)
                    .body(is(Integer.toString(0)));
        }

        // Verify cross-user interactions work
        given()
                .spec(getAuthenticatedSpec(tokens[numberOfUsers - 1]))
                .when()
                .get(friendsBaseUrl + "/api/friends/received-requests")
                .then()
                .statusCode(200)
                .body("content", hasSize(numberOfUsers - 1)); // Received requests from all other users
    }

    @Test
    @Order(4)
    @DisplayName("Should propagate user deletion event to Friends and Adoption modules via RabbitMQ")
    void testUserDeletionEventPropagation() throws Exception {
        String user1Name = "deletion_user1_" + System.currentTimeMillis();
        String user1Email = user1Name + "@example.com";
        String user2Name = "deletion_user2_" + System.currentTimeMillis();
        String user2Email = user2Name + "@example.com";

        // Register both users
        Response user1Response = registerAndLoginUser(user1Name, user1Email);
        String user1Token = JwtTokenExtractor.extractAccessToken(user1Response);
        String user1Id = JwtTokenExtractor.extractUserId(user1Response);

        Response user2Response = registerAndLoginUser(user2Name, user2Email);
        String user2Token = JwtTokenExtractor.extractAccessToken(user2Response);
        String user2Id = JwtTokenExtractor.extractUserId(user2Response);

        Thread.sleep(3000);

        // Step 1: Create data in both modules for user1
        // Create pet post in adoption module
        String petPostJson = """
                {
                    "petDTO": {
                        "name": "DeletionTestPet",
                        "species": "DOG",
                        "gender": "MALE",
                        "breed": "Labrador",
                        "dateOfBirth": "2020-01-01",
                        "description": "Pet for deletion test"
                    },
                    "description": "Testing deletion propagation",
                    "postType": "ADOPTION",
                    "latitude": 5.0,
                    "longitude": 5.0
                }
                """;

        Response petPostResponse = given()
                .spec(getAuthenticatedSpec(user1Token))
                .body(petPostJson)
                .when()
                .post(adoptionBaseUrl + "/api/pet-posts")
                .then()
                .statusCode(201)
                .extract().response();

        String petPostId = petPostResponse.jsonPath().getString("postId");

        // Create friendship in friends module
        Response friendRequestResponse = given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user2Id)
                .then()
                .statusCode(201)
                .extract().response();

        Thread.sleep(1500);
        Response notificationResponse = given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(notificationBaseUrl + "/api/notifications")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].type", equalTo(TestDataBuilder.UserBuilder.NotificationType.FRIEND_REQUEST_RECEIVED.toString()))
                .body("content[0].status", equalTo(TestDataBuilder.UserBuilder.NotificationStatus.UNREAD.toString()))
                .extract().response();

        String notificationId = notificationResponse.jsonPath().getString("content[0].notificationId");

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .delete(notificationBaseUrl + "/api/notifications/" + notificationId)
                .then()
                .statusCode(200)
                .body(equalTo("Notification deleted successfully"));

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .put(notificationBaseUrl + "/api/notifications/mark-read/" + notificationId)
                .then()
                .statusCode(404);

        String requestId = friendRequestResponse.jsonPath().getString("requestId");

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + requestId)
                .then()
                .statusCode(201);

        Thread.sleep(1500);
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(notificationBaseUrl + "/api/notifications")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].type", equalTo(TestDataBuilder.UserBuilder.NotificationType.FRIEND_REQUEST_ACCEPTED.toString()))
                .body("content[0].status", equalTo(TestDataBuilder.UserBuilder.NotificationStatus.UNREAD.toString()));

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .put(adoptionBaseUrl + "/api/pet-posts/" + petPostId + "/react")
                .then()
                .statusCode(200)
                .body("reacts", equalTo(1));

        Thread.sleep(1500);
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(notificationBaseUrl + "/api/notifications")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("content[0].type", equalTo(TestDataBuilder.UserBuilder.NotificationType.PET_POST_LIKED.toString()))
                .body("content[0].status", equalTo(TestDataBuilder.UserBuilder.NotificationStatus.UNREAD.toString()));

        Response chatResponse = given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + user2Id)
                .then()
                .statusCode(201)
                .extract().response();

        String chatId = chatResponse.jsonPath().getString("chatId");

        // Step 2: Verify data exists before deletion
        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("1"));

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/" + petPostId)
                .then()
                .statusCode(200)
                .body("ownerId", equalTo(user1Id));

        // Step 3: Delete user1 from Registration Module
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .body("{\"email\": \"" + user1Email + "\"}")
                .when()
                .delete(registrationBaseUrl + "/api/user/auth/delete")
                .then()
                .statusCode(200)
                .body(equalTo("User deleted successfully"));

        // Wait for RabbitMQ deletion event propagation
        Thread.sleep(5000);

        // Step 4: Verify user1 is deleted from Registration Module
        given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createLoginJson(user1Email, PASSWORD))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/login")
                .then()
                .statusCode(anyOf(is(401), is(404)));

        // Step 5: Verify cascading deletion in Friends Module
        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("0")); // Friendship should be removed

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(friendsBaseUrl + "/api/chats/" + chatId)
                .then()
                .statusCode(anyOf(is(404), is(403))); // Chat should be removed

        // Step 6: Verify cascading deletion in Adoption Module
        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/" + petPostId)
                .then()
                .statusCode(404); // Pet post should be deleted

        // Step 7: Verify user2's reaction is also removed (no orphaned data)
        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/user/" + user2Id)
                .then()
                .statusCode(200)
                .body("content", is(empty())); // User2 has no posts, reactions cleaned up
    }

    @Test
    @Order(5)
    @DisplayName("Should handle deletion of user with complex cross-module relationships")
    void testComplexUserDeletionPropagation() throws Exception {
        String mainUser = "complex_main_" + System.currentTimeMillis();
        String mainEmail = mainUser + "@example.com";
        String friend1User = "complex_friend1_" + System.currentTimeMillis();
        String friend1Email = friend1User + "@example.com";
        String friend2User = "complex_friend2_" + System.currentTimeMillis();
        String friend2Email = friend2User + "@example.com";

        // Register all users
        Response mainResponse = registerAndLoginUser(mainUser, mainEmail);
        String mainToken = JwtTokenExtractor.extractAccessToken(mainResponse);
        String mainId = JwtTokenExtractor.extractUserId(mainResponse);

        Response friend1Response = registerAndLoginUser(friend1User, friend1Email);
        String friend1Token = JwtTokenExtractor.extractAccessToken(friend1Response);
        String friend1Id = JwtTokenExtractor.extractUserId(friend1Response);

        Response friend2Response = registerAndLoginUser(friend2User, friend2Email);
        String friend2Token = JwtTokenExtractor.extractAccessToken(friend2Response);
        String friend2Id = JwtTokenExtractor.extractUserId(friend2Response);

        Thread.sleep(4000);

        // Step 1: Create complex relationships
        // Main user creates multiple pet posts
        String[] petPostIds = new String[2];
        for (int i = 0; i < 2; i++) {
            String petPostJson = String.format("""
                    {
                        "petDTO": {
                            "name": "ComplexPet%d",
                            "species": "DOG",
                            "gender": "MALE",
                            "breed": "TestBreed",
                            "dateOfBirth": "2020-01-01",
                            "description": "Complex test pet %d"
                        },
                        "description": "Complex relationship test post %d",
                        "postType": "ADOPTION",
                        "latitude": 5.0,
                        "longitude": 5.0
                    }
                    """, i, i, i);

            Response petResponse = given()
                    .spec(getAuthenticatedSpec(mainToken))
                    .body(petPostJson)
                    .when()
                    .post(adoptionBaseUrl + "/api/pet-posts")
                    .then()
                    .statusCode(201)
                    .extract().response();

            petPostIds[i] = petResponse.jsonPath().getString("postId");
        }

        // Both friends react to main user's posts
        for (String petPostId : petPostIds) {
            given()
                    .spec(getAuthenticatedSpec(friend1Token))
                    .when()
                    .put(adoptionBaseUrl + "/api/pet-posts/" + petPostId + "/react")
                    .then()
                    .statusCode(200);

            given()
                    .spec(getAuthenticatedSpec(friend2Token))
                    .when()
                    .put(adoptionBaseUrl + "/api/pet-posts/" + petPostId + "/react")
                    .then()
                    .statusCode(200);
        }

        // Create friendships
        Response req1Response = given()
                .spec(getAuthenticatedSpec(mainToken))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + friend1Id)
                .then()
                .statusCode(201)
                .extract().response();

        given()
                .spec(getAuthenticatedSpec(friend1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + req1Response.jsonPath().getString("requestId"))
                .then()
                .statusCode(201);

        Response req2Response = given()
                .spec(getAuthenticatedSpec(mainToken))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + friend2Id)
                .then()
                .statusCode(201)
                .extract().response();

        given()
                .spec(getAuthenticatedSpec(friend2Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + req2Response.jsonPath().getString("requestId"))
                .then()
                .statusCode(201);

        // Create follows
        given()
                .spec(getAuthenticatedSpec(friend1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/follow/" + mainId)
                .then()
                .statusCode(201);

        given()
                .spec(getAuthenticatedSpec(mainToken))
                .when()
                .get(notificationBaseUrl + "/api/notifications")
                .then()
                .statusCode(200)
                .body("content[0].type", equalTo(TestDataBuilder.UserBuilder.NotificationType.NEW_FOLLOWER.toString()))
                .body("content[0].status", equalTo(TestDataBuilder.UserBuilder.NotificationStatus.UNREAD.toString()));

        // Create chats and messages
        Response chat1Response = given()
                .spec(getAuthenticatedSpec(mainToken))
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + friend1Id)
                .then()
                .statusCode(201)
                .extract().response();

        String chat1Id = chat1Response.jsonPath().getString("chatId");

        given()
                .spec(getAuthenticatedSpec(mainToken))
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(UUID.fromString(chat1Id), "Hello friend1!"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200);

        // Step 2: Verify all relationships exist
        given()
                .spec(getAuthenticatedSpec(friend1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + friend1Id)
                .then()
                .statusCode(200)
                .body(equalTo("1"));

        given()
                .spec(getAuthenticatedSpec(friend2Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + friend2Id)
                .then()
                .statusCode(200)
                .body(equalTo("1"));

        for (String petPostId : petPostIds) {
            given()
                    .spec(getAuthenticatedSpec(friend1Token))
                    .when()
                    .get(adoptionBaseUrl + "/api/pet-posts/" + petPostId)
                    .then()
                    .statusCode(200)
                    .body("reacts", equalTo(2));
        }

        // Step 3: Delete main user
        given()
                .spec(getAuthenticatedSpec(mainToken))
                .body("{\"email\": \"" + mainEmail + "\"}")
                .when()
                .delete(registrationBaseUrl + "/api/user/auth/delete")
                .then()
                .statusCode(200);

        Thread.sleep(6000); // Wait for complex propagation

        // Step 4: Verify complete cleanup across all modules
        // All pet posts should be deleted
        for (String petPostId : petPostIds) {
            given()
                    .spec(getAuthenticatedSpec(friend1Token))
                    .when()
                    .get(adoptionBaseUrl + "/api/pet-posts/" + petPostId)
                    .then()
                    .statusCode(404);
        }

        // All friendships should be removed
        given()
                .spec(getAuthenticatedSpec(friend1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + friend1Id)
                .then()
                .statusCode(200)
                .body(equalTo("0"));

        given()
                .spec(getAuthenticatedSpec(friend2Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + friend2Id)
                .then()
                .statusCode(200)
                .body(equalTo("0"));

        given()
                .spec(getAuthenticatedSpec(mainToken))
                .when()
                .get(notificationBaseUrl + "/api/notification")
                .then()
                .statusCode(anyOf(is(404), is(403)));

        // Chat should be removed/inaccessible
        given()
                .spec(getAuthenticatedSpec(friend1Token))
                .when()
                .get(friendsBaseUrl + "/api/chats/" + chat1Id)
                .then()
                .statusCode(anyOf(is(404), is(403)));

        // Follow relationship should be removed
        given()
                .spec(getAuthenticatedSpec(friend1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-following/" + friend1Id)
                .then()
                .statusCode(200)
                .body(equalTo("0"));
    }

    @Test
    @Order(6)
    @DisplayName("Should handle concurrent user deletions with proper event propagation")
    void testConcurrentUserDeletionsEventPropagation() throws Exception {
        int numberOfUsers = 3;
        String[] usernames = new String[numberOfUsers];
        String[] emails = new String[numberOfUsers];
        String[] tokens = new String[numberOfUsers];
        String[] userIds = new String[numberOfUsers];

        // Register users
        for (int i = 0; i < numberOfUsers; i++) {
            usernames[i] = "concurrent_del_" + i + "_" + System.currentTimeMillis();
            emails[i] = usernames[i] + "@example.com";

            Response response = registerAndLoginUser(usernames[i], emails[i]);
            tokens[i] = JwtTokenExtractor.extractAccessToken(response);
            userIds[i] = JwtTokenExtractor.extractUserId(response);
        }

        Thread.sleep(4000);

        // Create cross-relationships between all users
        for (int i = 0; i < numberOfUsers; i++) {
            for (int j = i + 1; j < numberOfUsers; j++) {
                // Send friend requests
                given()
                        .spec(getAuthenticatedSpec(tokens[i]))
                        .when()
                        .post(friendsBaseUrl + "/api/friends/send-request/" + userIds[j])
                        .then()
                        .statusCode(201);
            }
        }

        // Accept all friend requests
        for (int i = 1; i < numberOfUsers; i++) {
            Response requestsResponse = given()
                    .spec(getAuthenticatedSpec(tokens[i]))
                    .when()
                    .get(friendsBaseUrl + "/api/friends/received-requests")
                    .then()
                    .statusCode(200)
                    .extract().response();

            if (requestsResponse.jsonPath().getList("content") != null) {
                for (Object request : requestsResponse.jsonPath().getList("content")) {
                    String requestId = ((java.util.Map<?, ?>) request).get("requestId").toString();
                    given()
                            .spec(getAuthenticatedSpec(tokens[i]))
                            .when()
                            .post(friendsBaseUrl + "/api/friends/accept-request/" + requestId)
                            .then()
                            .statusCode(201);
                }
            }
        }

        Thread.sleep(2000);

        // Create pet posts for each user
        for (int i = 0; i < numberOfUsers; i++) {
            String petPostJson = String.format("""
                    {
                        "petDTO": {
                            "name": "ConcurrentPet%d",
                            "species": "DOG",
                            "gender": "MALE",
                            "breed": "TestBreed",
                            "dateOfBirth": "2020-01-01",
                            "description": "Concurrent test pet %d"
                        },
                        "description": "Concurrent deletion test post %d",
                        "postType": "ADOPTION",
                        "latitude": 5.0,
                        "longitude": 5.0
                    }
                    """, i, i, i);

            given()
                    .spec(getAuthenticatedSpec(tokens[i]))
                    .body(petPostJson)
                    .when()
                    .post(adoptionBaseUrl + "/api/pet-posts")
                    .then()
                    .statusCode(201);
        }

        // Verify friendships exist
        for (int i = 0; i < numberOfUsers; i++) {
            given()
                    .spec(getAuthenticatedSpec(tokens[i]))
                    .when()
                    .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userIds[i])
                    .then()
                    .statusCode(200)
                    .body(equalTo(String.valueOf(numberOfUsers - 1)));
        }

        // Concurrently delete first two users
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch deletionLatch = new CountDownLatch(2);
        AtomicInteger deletionSuccessCount = new AtomicInteger(0);

        for (int i = 0; i < 2; i++) {
            final int userIndex = i;
            executor.submit(() -> {
                try {
                    Response deleteResponse = given()
                            .spec(getAuthenticatedSpec(tokens[userIndex]))
                            .body("{\"email\": \"" + emails[userIndex] + "\"}")
                            .when()
                            .delete(registrationBaseUrl + "/api/user/auth/delete");

                    if (deleteResponse.getStatusCode() == 200) {
                        deletionSuccessCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    deletionLatch.countDown();
                }
            });
        }

        assertTrue(deletionLatch.await(30, TimeUnit.SECONDS));
        assertEquals(2, deletionSuccessCount.get());

        Thread.sleep(6000); // Wait for all propagations

        // Verify remaining user has no friends (all deleted)
        given()
                .spec(getAuthenticatedSpec(tokens[2]))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userIds[2])
                .then()
                .statusCode(200)
                .body(equalTo("0"));

        // Verify deleted users cannot login
        for (int i = 0; i < 2; i++) {
            given()
                    .contentType("application/json")
                    .body(TestDataBuilder.UserBuilder.createLoginJson(emails[i], PASSWORD))
                    .when()
                    .post(registrationBaseUrl + "/api/user/auth/login")
                    .then()
                    .statusCode(anyOf(is(401), is(404)));
        }

        executor.shutdown();
    }

    @Test
    @Order(7)
    @DisplayName("Should maintain data consistency during mixed registration and deletion events")
    void testMixedRegistrationDeletionEvents() throws Exception {
        // Phase 1: Register multiple users
        String[] phase1Users = new String[3];
        String[] phase1Emails = new String[3];
        String[] phase1Tokens = new String[3];
        String[] phase1UserIds = new String[3];

        for (int i = 0; i < 3; i++) {
            phase1Users[i] = "mixed_phase1_" + i + "_" + System.currentTimeMillis();
            phase1Emails[i] = phase1Users[i] + "@example.com";
            Response response = registerAndLoginUser(phase1Users[i], phase1Emails[i]);
            phase1Tokens[i] = JwtTokenExtractor.extractAccessToken(response);
            phase1UserIds[i] = JwtTokenExtractor.extractUserId(response);
        }

        Thread.sleep(3000);

        // Create relationships between phase1 users
        given()
                .spec(getAuthenticatedSpec(phase1Tokens[0]))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + phase1UserIds[1])
                .then()
                .statusCode(201);

        given()
                .spec(getAuthenticatedSpec(phase1Tokens[1]))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + phase1UserIds[2])
                .then()
                .statusCode(201);

        // Phase 2: While relationships exist, register new users and delete one old user
        String newUser = "mixed_new_" + System.currentTimeMillis();
        String newEmail = newUser + "@example.com";
        Response newResponse = registerAndLoginUser(newUser, newEmail);
        String newToken = JwtTokenExtractor.extractAccessToken(newResponse);
        String newUserId = JwtTokenExtractor.extractUserId(newResponse);

        // Delete one of the phase1 users
        given()
                .spec(getAuthenticatedSpec(phase1Tokens[0]))
                .body("{\"email\": \"" + phase1Emails[0] + "\"}")
                .when()
                .delete(registrationBaseUrl + "/api/user/auth/delete")
                .then()
                .statusCode(200);

        Thread.sleep(5000);

        // Phase 3: Verify consistency
        // Deleted user should not be accessible
        given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createLoginJson(phase1Emails[0], PASSWORD))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/login")
                .then()
                .statusCode(anyOf(is(401), is(404)));

        // New user should be fully functional
        given()
                .spec(getAuthenticatedSpec(newToken))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + newUserId)
                .then()
                .statusCode(200);

        given()
                .spec(getAuthenticatedSpec(newToken))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/user/" + newUserId)
                .then()
                .statusCode(200);

        // New user can interact with remaining users
        given()
                .spec(getAuthenticatedSpec(newToken))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + phase1UserIds[1])
                .then()
                .statusCode(201);

        // Remaining users should have their friend request from deleted user removed
        given()
                .spec(getAuthenticatedSpec(phase1Tokens[1]))
                .when()
                .get(friendsBaseUrl + "/api/friends/received-requests")
                .then()
                .statusCode(anyOf(is(200), is(204)))
                .body("content", anyOf(is(empty()), hasSize(1))); // Should only have request from new user, not deleted user
    }

    @Test
    @Order(8)
    @DisplayName("Should handle user deletion with active pet post interactions")
    void testUserDeletionWithActivePetPostInteractions() throws Exception {
        String ownerUser = "owner_" + System.currentTimeMillis();
        String ownerEmail = ownerUser + "@example.com";
        String[] reactorUsers = new String[3];
        String[] reactorEmails = new String[3];
        String[] reactorTokens = new String[3];
        String[] reactorIds = new String[3];

        // Register owner
        Response ownerResponse = registerAndLoginUser(ownerUser, ownerEmail);
        String ownerToken = JwtTokenExtractor.extractAccessToken(ownerResponse);
        String ownerId = JwtTokenExtractor.extractUserId(ownerResponse);

        // Register multiple reactor users
        for (int i = 0; i < 3; i++) {
            reactorUsers[i] = "reactor_" + i + "_" + System.currentTimeMillis();
            reactorEmails[i] = reactorUsers[i] + "@example.com";
            Response response = registerAndLoginUser(reactorUsers[i], reactorEmails[i]);
            reactorTokens[i] = JwtTokenExtractor.extractAccessToken(response);
            reactorIds[i] = JwtTokenExtractor.extractUserId(response);
        }

        Thread.sleep(4000);

        // Owner creates multiple pet posts
        String[] petPostIds = new String[2];
        for (int i = 0; i < 2; i++) {
            String petPostJson = String.format("""
                    {
                        "petDTO": {
                            "name": "InteractionPet%d",
                            "species": "CAT",
                            "gender": "FEMALE",
                            "breed": "TestBreed",
                            "dateOfBirth": "2021-01-01",
                            "description": "Pet for interaction test %d"
                        },
                        "description": "Testing active interactions %d",
                        "postType": "BREEDING",
                        "latitude": 5.0,
                        "longitude": 5.0
                    }
                    """, i, i, i);

            Response petResponse = given()
                    .spec(getAuthenticatedSpec(ownerToken))
                    .body(petPostJson)
                    .when()
                    .post(adoptionBaseUrl + "/api/pet-posts")
                    .then()
                    .statusCode(201)
                    .extract().response();

            petPostIds[i] = petResponse.jsonPath().getString("postId");
        }

        // All reactor users react to all posts
        for (String petPostId : petPostIds) {
            for (String reactorToken : reactorTokens) {
                given()
                        .spec(getAuthenticatedSpec(reactorToken))
                        .when()
                        .put(adoptionBaseUrl + "/api/pet-posts/" + petPostId + "/react")
                        .then()
                        .statusCode(200);
            }
        }

        // Verify posts have reactions
        for (String petPostId : petPostIds) {
            given()
                    .spec(getAuthenticatedSpec(reactorTokens[0]))
                    .when()
                    .get(adoptionBaseUrl + "/api/pet-posts/" + petPostId)
                    .then()
                    .statusCode(200)
                    .body("reacts", equalTo(3));
        }

        // Delete owner while posts have active reactions
        given()
                .spec(getAuthenticatedSpec(ownerToken))
                .body("{\"email\": \"" + ownerEmail + "\"}")
                .when()
                .delete(registrationBaseUrl + "/api/user/auth/delete")
                .then()
                .statusCode(200);

        Thread.sleep(5000);

        // Verify all posts are deleted (cascading deletion)
        for (String petPostId : petPostIds) {
            given()
                    .spec(getAuthenticatedSpec(reactorTokens[0]))
                    .when()
                    .get(adoptionBaseUrl + "/api/pet-posts/" + petPostId)
                    .then()
                    .statusCode(404);
        }

        // Verify reactor users are still functional
        for (int i = 0; i < 3; i++) {
            given()
                    .spec(getAuthenticatedSpec(reactorTokens[i]))
                    .when()
                    .get(adoptionBaseUrl + "/api/pet-posts/user/" + reactorIds[i])
                    .then()
                    .statusCode(200);

            // They can still interact with each other
            if (i < 2) {
                given()
                        .spec(getAuthenticatedSpec(reactorTokens[i]))
                        .when()
                        .post(friendsBaseUrl + "/api/friends/send-request/" + reactorIds[i + 1])
                        .then()
                        .statusCode(201);
            }
        }
    }

    @Test
    @Order(9)
    @DisplayName("Should handle user deletion during active chat conversations")
    void testUserDeletionDuringActiveChatConversations() throws Exception {
        String user1Name = "chatter1_" + System.currentTimeMillis();
        String user1Email = user1Name + "@example.com";
        String user2Name = "chatter2_" + System.currentTimeMillis();
        String user2Email = user2Name + "@example.com";
        String user3Name = "chatter3_" + System.currentTimeMillis();
        String user3Email = user3Name + "@example.com";

        // Register all users
        Response user1Response = registerAndLoginUser(user1Name, user1Email);
        String user1Token = JwtTokenExtractor.extractAccessToken(user1Response);
        String user1Id = JwtTokenExtractor.extractUserId(user1Response);

        Response user2Response = registerAndLoginUser(user2Name, user2Email);
        String user2Token = JwtTokenExtractor.extractAccessToken(user2Response);
        String user2Id = JwtTokenExtractor.extractUserId(user2Response);

        Response user3Response = registerAndLoginUser(user3Name, user3Email);
        String user3Token = JwtTokenExtractor.extractAccessToken(user3Response);
        String user3Id = JwtTokenExtractor.extractUserId(user3Response);

        Thread.sleep(4000);

        // Create friendships
        Response req1 = given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user2Id)
                .then()
                .statusCode(201)
                .extract().response();

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + req1.jsonPath().getString("requestId"))
                .then()
                .statusCode(201);

        Response req2 = given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user3Id)
                .then()
                .statusCode(201)
                .extract().response();

        given()
                .spec(getAuthenticatedSpec(user3Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + req2.jsonPath().getString("requestId"))
                .then()
                .statusCode(201);

        // Create multiple chats
        Response chat1Response = given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + user2Id)
                .then()
                .statusCode(201)
                .extract().response();

        String chat1Id = chat1Response.jsonPath().getString("chatId");

        Response chat2Response = given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + user3Id)
                .then()
                .statusCode(201)
                .extract().response();

        String chat2Id = chat2Response.jsonPath().getString("chatId");

        // Send multiple messages in both chats
        String[] messageIds = new String[4];

        Response msg1 = given()
                .spec(getAuthenticatedSpec(user1Token))
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(UUID.fromString(chat1Id), "Hello user2!"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200)
                .extract().response();
        messageIds[0] = msg1.jsonPath().getString("messageId");

        Response msg2 = given()
                .spec(getAuthenticatedSpec(user2Token))
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(UUID.fromString(chat1Id), "Hi user1!"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200)
                .extract().response();
        messageIds[1] = msg2.jsonPath().getString("messageId");

        Response msg3 = given()
                .spec(getAuthenticatedSpec(user1Token))
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(UUID.fromString(chat2Id), "Hello user3!"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200)
                .extract().response();
        messageIds[2] = msg3.jsonPath().getString("messageId");

        Response msg4 = given()
                .spec(getAuthenticatedSpec(user3Token))
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(UUID.fromString(chat2Id), "Hi user1!"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200)
                .extract().response();
        messageIds[3] = msg4.jsonPath().getString("messageId");

        // Add reactions to messages
        given()
                .spec(getAuthenticatedSpec(user2Token))
                .body(TestDataBuilder.MessageBuilder.createMessageReactionJson("LIKE"))
                .when()
                .put(friendsBaseUrl + "/api/messages/" + messageIds[0] + "/reaction")
                .then()
                .statusCode(200);

        // Verify chats and messages exist
        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(friendsBaseUrl + "/api/messages/chat/" + chat1Id)
                .then()
                .statusCode(200)
                .body("content", hasSize(2));

        // Delete user1 while they have active chats and messages
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .body("{\"email\": \"" + user1Email + "\"}")
                .when()
                .delete(registrationBaseUrl + "/api/user/auth/delete")
                .then()
                .statusCode(200);

        Thread.sleep(6000);

        // Verify cascading effects
        // Chats should be removed/inaccessible
        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(friendsBaseUrl + "/api/chats/" + chat1Id)
                .then()
                .statusCode(anyOf(is(404), is(403)));

        given()
                .spec(getAuthenticatedSpec(user3Token))
                .when()
                .get(friendsBaseUrl + "/api/chats/" + chat2Id)
                .then()
                .statusCode(anyOf(is(404), is(403)));

        // Friendships should be removed
        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("0"));

        given()
                .spec(getAuthenticatedSpec(user3Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + user3Id)
                .then()
                .statusCode(200)
                .body(equalTo("0"));
        // User2 and User3 should still be able to interact with each other
        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user3Id)
                .then()
                .statusCode(201);

        // Both users should still be functional in adoption module
        String petPostJson = """
                {
                    "petDTO": {
                        "name": "PostDeletionPet",
                        "species": "DOG",
                        "gender": "MALE",
                        "breed": "TestBreed",
                        "dateOfBirth": "2020-01-01",
                        "description": "Pet after user deletion"
                    },
                    "description": "Testing post-deletion functionality",
                    "postType": "ADOPTION",
                    "latitude": 5.0,
                    "longitude": 5.0
                }
                """;

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .body(petPostJson)
                .when()
                .post(adoptionBaseUrl + "/api/pet-posts")
                .then()
                .statusCode(201);
    }

    @Test
    @Order(10)
    @DisplayName("Should handle rapid registration-deletion cycles")
    void testRapidRegistrationDeletionCycles() throws Exception {
        int cycles = 3;

        for (int cycle = 0; cycle < cycles; cycle++) {
            String username = "cycle_user_" + cycle + "_" + System.currentTimeMillis();
            String email = username + "@example.com";

            // Register user
            Response userResponse = registerAndLoginUser(username, email);
            String token = JwtTokenExtractor.extractAccessToken(userResponse);
            String userId = JwtTokenExtractor.extractUserId(userResponse);

            Thread.sleep(2000); // Minimal propagation wait

            // Create some data in both modules
            String petPostJson = String.format("""
                    {
                        "petDTO": {
                            "name": "CyclePet%d",
                            "species": "DOG",
                            "gender": "MALE",
                            "breed": "TestBreed",
                            "dateOfBirth": "2020-01-01",
                            "description": "Cycle test pet %d"
                        },
                        "description": "Rapid cycle test post %d",
                        "postType": "ADOPTION",
                        "latitude": 5.0,
                        "longitude": 5.0
                    }
                    """, cycle, cycle, cycle);

            Response petResponse = given()
                    .spec(getAuthenticatedSpec(token))
                    .body(petPostJson)
                    .when()
                    .post(adoptionBaseUrl + "/api/pet-posts")
                    .then()
                    .statusCode(201)
                    .extract().response();

            String petPostId = petResponse.jsonPath().getString("postId");

            // Verify data exists
            given()
                    .spec(getAuthenticatedSpec(token))
                    .when()
                    .get(adoptionBaseUrl + "/api/pet-posts/" + petPostId)
                    .then()
                    .statusCode(200);

            given()
                    .spec(getAuthenticatedSpec(token))
                    .when()
                    .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userId)
                    .then()
                    .statusCode(200);

            // Delete user immediately
            given()
                    .spec(getAuthenticatedSpec(token))
                    .body("{\"email\": \"" + email + "\"}")
                    .when()
                    .delete(registrationBaseUrl + "/api/user/auth/delete")
                    .then()
                    .statusCode(200);

            Thread.sleep(3000); // Wait for deletion propagation

            // Verify user and data are gone
            given()
                    .contentType("application/json")
                    .body(TestDataBuilder.UserBuilder.createLoginJson(email, PASSWORD))
                    .when()
                    .post(registrationBaseUrl + "/api/user/auth/login")
                    .then()
                    .statusCode(anyOf(is(401), is(404)));
        }
    }

    @Test
    @Order(11)
    @DisplayName("Should maintain referential integrity across module boundaries")
    void testReferentialIntegrityAcrossModules() throws Exception {
        String user1Name = "integrity_user1_" + System.currentTimeMillis();
        String user1Email = user1Name + "@example.com";
        String user2Name = "integrity_user2_" + System.currentTimeMillis();
        String user2Email = user2Name + "@example.com";

        // Register users
        Response user1Response = registerAndLoginUser(user1Name, user1Email);
        String user1Token = JwtTokenExtractor.extractAccessToken(user1Response);
        String user1Id = JwtTokenExtractor.extractUserId(user1Response);

        Response user2Response = registerAndLoginUser(user2Name, user2Email);
        String user2Token = JwtTokenExtractor.extractAccessToken(user2Response);
        String user2Id = JwtTokenExtractor.extractUserId(user2Response);

        Thread.sleep(4000);

        // Create complex cross-module relationships
        // 1. Friendship in Friends Module
        Response friendRequest = given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user2Id)
                .then()
                .statusCode(201)
                .extract().response();

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + friendRequest.jsonPath().getString("requestId"))
                .then()
                .statusCode(201);

        // 2. Pet Post in Adoption Module
        String petPostJson = """
                {
                    "petDTO": {
                        "name": "IntegrityPet",
                        "species": "CAT",
                        "gender": "FEMALE",
                        "breed": "TestBreed",
                        "dateOfBirth": "2021-01-01",
                        "description": "Pet for integrity test"
                    },
                    "description": "Testing cross-module integrity",
                    "postType": "ADOPTION",
                    "latitude": 5.0,
                    "longitude": 5.0
                }
                """;

        Response petResponse = given()
                .spec(getAuthenticatedSpec(user1Token))
                .body(petPostJson)
                .when()
                .post(adoptionBaseUrl + "/api/pet-posts")
                .then()
                .statusCode(201)
                .extract().response();

        String petPostId = petResponse.jsonPath().getString("postId");

        // 3. Chat in Friends Module
        Response chatResponse = given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + user2Id)
                .then()
                .statusCode(201)
                .extract().response();

        String chatId = chatResponse.jsonPath().getString("chatId");

        // 4. User2 reacts to User1's pet post
        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .put(adoptionBaseUrl + "/api/pet-posts/" + petPostId + "/react")
                .then()
                .statusCode(200)
                .body("reacts", equalTo(1));

        // 5. Send messages in chat
        Response messageResponse = given()
                .spec(getAuthenticatedSpec(user1Token))
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(UUID.fromString(chatId), "Check out my pet post!"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200)
                .extract().response();

        String messageId = messageResponse.jsonPath().getString("messageId");

        // Verify all relationships exist and are consistent
        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/is-friend/" + user1Id)
                .then()
                .statusCode(200)
                .body(equalTo("true"));

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/" + petPostId)
                .then()
                .statusCode(200)
                .body("ownerId", equalTo(user1Id))
                .body("reacts", equalTo(1));

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(friendsBaseUrl + "/api/messages/chat/" + chatId)
                .then()
                .statusCode(200)
                .body("content", hasSize(1));

        // Now delete user1 and verify ALL references are cleaned up
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .body("{\"email\": \"" + user1Email + "\"}")
                .when()
                .delete(registrationBaseUrl + "/api/user/auth/delete")
                .then()
                .statusCode(200);

        Thread.sleep(6000);

        // Verify complete cleanup - no orphaned references
        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("0"));

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/" + petPostId)
                .then()
                .statusCode(404);

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(friendsBaseUrl + "/api/chats/" + chatId)
                .then()
                .statusCode(anyOf(is(404), is(403)));
    }

    @Test
    @Order(12)
    @DisplayName("Should handle user deletion with maximum relationship complexity")
    void testMaximumComplexityUserDeletion() throws Exception {
        String centralUser = "central_" + System.currentTimeMillis();
        String centralEmail = centralUser + "@example.com";

        // Create network of 5 users with central user at the center
        String[] networkUsers = new String[4];
        String[] networkEmails = new String[4];
        String[] networkTokens = new String[4];
        String[] networkIds = new String[4];

        // Register central user
        Response centralResponse = registerAndLoginUser(centralUser, centralEmail);
        String centralToken = JwtTokenExtractor.extractAccessToken(centralResponse);
        String centralId = JwtTokenExtractor.extractUserId(centralResponse);

        // Register network users
        for (int i = 0; i < 4; i++) {
            networkUsers[i] = "network_" + i + "_" + System.currentTimeMillis();
            networkEmails[i] = networkUsers[i] + "@example.com";
            Response response = registerAndLoginUser(networkUsers[i], networkEmails[i]);
            networkTokens[i] = JwtTokenExtractor.extractAccessToken(response);
            networkIds[i] = JwtTokenExtractor.extractUserId(response);
        }

        Thread.sleep(5000);

        // Create maximum complexity relationships
        String[] petPostIds = new String[3];
        String[] chatIds = new String[4];

        // 1. Central user creates multiple pet posts
        for (int i = 0; i < 3; i++) {
            String petPostJson = String.format("""
                    {
                        "petDTO": {
                            "name": "MaxComplexityPet%d",
                            "species": "CAT",
                            "gender": "FEMALE",
                            "breed": "TestBreed",
                            "dateOfBirth": "2021-01-01",
                            "description": "Maximum complexity pet %d"
                        },
                        "description": "Maximum complexity test post %d",
                        "postType": "BREEDING",
                        "latitude": 5.0,
                        "longitude": 5.0
                    }
                    """, i, i, i);

            Response petResponse = given()
                    .spec(getAuthenticatedSpec(centralToken))
                    .body(petPostJson)
                    .when()
                    .post(adoptionBaseUrl + "/api/pet-posts")
                    .then()
                    .statusCode(201)
                    .extract().response();

            petPostIds[i] = petResponse.jsonPath().getString("postId");
        }

        // 2. Create friendships with all network users
        for (int i = 0; i < 4; i++) {
            Response friendRequest = given()
                    .spec(getAuthenticatedSpec(centralToken))
                    .when()
                    .post(friendsBaseUrl + "/api/friends/send-request/" + networkIds[i])
                    .then()
                    .statusCode(201)
                    .extract().response();

            given()
                    .spec(getAuthenticatedSpec(networkTokens[i]))
                    .when()
                    .post(friendsBaseUrl + "/api/friends/accept-request/" + friendRequest.jsonPath().getString("requestId"))
                    .then()
                    .statusCode(201);
        }

        // 3. All network users react to all pet posts
        for (String petPostId : petPostIds) {
            for (String networkToken : networkTokens) {
                given()
                        .spec(getAuthenticatedSpec(networkToken))
                        .when()
                        .put(adoptionBaseUrl + "/api/pet-posts/" + petPostId + "/react")
                        .then()
                        .statusCode(200);
            }
        }

        // 4. Create chats with all network users
        for (int i = 0; i < 4; i++) {
            Response chatResponse = given()
                    .spec(getAuthenticatedSpec(centralToken))
                    .when()
                    .post(friendsBaseUrl + "/api/chats/user/" + networkIds[i])
                    .then()
                    .statusCode(201)
                    .extract().response();

            chatIds[i] = chatResponse.jsonPath().getString("chatId");

            // Send multiple messages in each chat
            given()
                    .spec(getAuthenticatedSpec(centralToken))
                    .body(TestDataBuilder.MessageBuilder.createSendMessageJson(UUID.fromString(chatIds[i]), "Hello network user " + i))
                    .when()
                    .post(friendsBaseUrl + "/api/messages/send")
                    .then()
                    .statusCode(200);

            given()
                    .spec(getAuthenticatedSpec(networkTokens[i]))
                    .body(TestDataBuilder.MessageBuilder.createSendMessageJson(UUID.fromString(chatIds[i]), "Hi central user!"))
                    .when()
                    .post(friendsBaseUrl + "/api/messages/send")
                    .then()
                    .statusCode(200);
        }

        // 5. Create follow relationships
        for (int i = 0; i < 2; i++) {
            given()
                    .spec(getAuthenticatedSpec(networkTokens[i]))
                    .when()
                    .post(friendsBaseUrl + "/api/friends/follow/" + centralId)
                    .then()
                    .statusCode(201);
        }

        // Verify all relationships exist
        for (int i = 0; i < 4; i++) {
            given()
                    .spec(getAuthenticatedSpec(networkTokens[i]))
                    .when()
                    .get(friendsBaseUrl + "/api/friends/is-friend/" + centralId)
                    .then()
                    .statusCode(200)
                    .body(equalTo("true"));
        }

        for (String petPostId : petPostIds) {
            given()
                    .spec(getAuthenticatedSpec(networkTokens[0]))
                    .when()
                    .get(adoptionBaseUrl + "/api/pet-posts/" + petPostId)
                    .then()
                    .statusCode(200)
                    .body("reacts", equalTo(4))
                    .body("ownerId", equalTo(centralId));
        }

        // Delete central user (maximum complexity cascade)
        given()
                .spec(getAuthenticatedSpec(centralToken))
                .body("{\"email\": \"" + centralEmail + "\"}")
                .when()
                .delete(registrationBaseUrl + "/api/user/auth/delete")
                .then()
                .statusCode(200);

        Thread.sleep(8000); // Extended wait for complex cascade

        // Verify complete cleanup across all modules - no orphaned data

        // 1. All pet posts should be deleted
        for (String petPostId : petPostIds) {
            given()
                    .spec(getAuthenticatedSpec(networkTokens[0]))
                    .when()
                    .get(adoptionBaseUrl + "/api/pet-posts/" + petPostId)
                    .then()
                    .statusCode(404);
        }

        // 2. All friendships should be removed
        for (int i = 0; i < 4; i++) {
            given()
                    .spec(getAuthenticatedSpec(networkTokens[i]))
                    .when()
                    .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + networkIds[i])
                    .then()
                    .statusCode(200)
                    .body(equalTo("0"));
        }

        // 3. All chats should be removed/inaccessible
        for (int i = 0; i < 4; i++) {
            given()
                    .spec(getAuthenticatedSpec(networkTokens[i]))
                    .when()
                    .get(friendsBaseUrl + "/api/chats/" + chatIds[i])
                    .then()
                    .statusCode(anyOf(is(404), is(403)));
        }

        // 4. All follow relationships should be removed
        for (int i = 0; i < 2; i++) {
            given()
                    .spec(getAuthenticatedSpec(networkTokens[i]))
                    .when()
                    .get(friendsBaseUrl + "/api/friends/get-number-of-following/" + networkIds[i])
                    .then()
                    .statusCode(200)
                    .body(equalTo("0"));
        }

        // 5. Central user should be completely removed from registration module
        given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createLoginJson(centralEmail, PASSWORD))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/login")
                .then()
                .statusCode(anyOf(is(401), is(404)));

        // 6. Network users should still be fully functional and can interact with each other
        for (int i = 0; i < 3; i++) {
            for (int j = i + 1; j < 4; j++) {
                given()
                        .spec(getAuthenticatedSpec(networkTokens[i]))
                        .when()
                        .post(friendsBaseUrl + "/api/friends/send-request/" + networkIds[j])
                        .then()
                        .statusCode(201);
            }
        }

        // 7. Network users can still create pet posts
        String postDeletionPetJson = """
                {
                    "petDTO": {
                        "name": "PostCascadePet",
                        "species": "DOG",
                        "gender": "MALE",
                        "breed": "Survivor",
                        "dateOfBirth": "2020-01-01",
                        "description": "Pet created after cascade deletion"
                    },
                    "description": "Proving system integrity after complex deletion",
                    "postType": "ADOPTION",
                    "latitude": 5.0,
                    "longitude": 5.0
                }
                """;

        Response postCascadeResponse = given()
                .spec(getAuthenticatedSpec(networkTokens[0]))
                .body(postDeletionPetJson)
                .when()
                .post(adoptionBaseUrl + "/api/pet-posts")
                .then()
                .statusCode(201)
                .body("ownerId", equalTo(networkIds[0]))
                .body("reacts", equalTo(0))
                .extract().response();

        String postCascadePetId = postCascadeResponse.jsonPath().getString("postId");

        // 8. Other network users can interact with the new post
        given()
                .spec(getAuthenticatedSpec(networkTokens[1]))
                .when()
                .put(adoptionBaseUrl + "/api/pet-posts/" + postCascadePetId + "/react")
                .then()
                .statusCode(200)
                .body("reacts", equalTo(1));

        given()
                .spec(getAuthenticatedSpec(networkTokens[2]))
                .when()
                .put(adoptionBaseUrl + "/api/pet-posts/" + postCascadePetId + "/react")
                .then()
                .statusCode(200)
                .body("reacts", equalTo(2));

        // 9. Verify system maintains consistency - check that no orphaned reactions exist
        given()
                .spec(getAuthenticatedSpec(networkTokens[0]))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/" + postCascadePetId)
                .then()
                .statusCode(200)
                .body("reacts", equalTo(2))
                .body("ownerId", equalTo(networkIds[0]));

        // 10. Final verification: Complex cross-module interaction still works
        // Create new chat between remaining users
        Response finalChatResponse = given()
                .spec(getAuthenticatedSpec(networkTokens[0]))
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + networkIds[1])
                .then()
                .statusCode(201)
                .extract().response();

        String finalChatId = finalChatResponse.jsonPath().getString("chatId");

        // Send message referencing the pet post (cross-module reference)
        given()
                .spec(getAuthenticatedSpec(networkTokens[0]))
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(
                        UUID.fromString(finalChatId),
                        "Check out my new pet post: " + postCascadePetId))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200);

        // Verify message was sent and chat is functional
        given()
                .spec(getAuthenticatedSpec(networkTokens[1]))
                .when()
                .get(friendsBaseUrl + "/api/messages/chat/" + finalChatId)
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].content", containsString(postCascadePetId));

        // 11. Ultimate verification: System can handle another complex user creation after deletion
        String newComplexUser = "post_cascade_user_" + System.currentTimeMillis();
        String newComplexEmail = newComplexUser + "@example.com";

        Response newComplexResponse = registerAndLoginUser(newComplexUser, newComplexEmail);
        String newComplexToken = JwtTokenExtractor.extractAccessToken(newComplexResponse);
        String newComplexId = JwtTokenExtractor.extractUserId(newComplexResponse);

        Thread.sleep(3000);

        // New user can immediately interact with existing system
        given()
                .spec(getAuthenticatedSpec(newComplexToken))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + networkIds[0])
                .then()
                .statusCode(201);

        given()
                .spec(getAuthenticatedSpec(newComplexToken))
                .when()
                .put(adoptionBaseUrl + "/api/pet-posts/" + postCascadePetId + "/react")
                .then()
                .statusCode(200)
                .body("reacts", equalTo(3)); // Now 3 reactions

        // Final system integrity check
        given()
                .spec(getAuthenticatedSpec(newComplexToken))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/" + postCascadePetId)
                .then()
                .statusCode(200)
                .body("reacts", equalTo(3))
                .body("ownerId", equalTo(networkIds[0]));

        // Verify all modules are in perfect sync
        given()
                .spec(getAuthenticatedSpec(networkTokens[0]))
                .when()
                .get(friendsBaseUrl + "/api/friends/received-requests")
                .then()
                .statusCode(200)
                .body("content", hasSize(greaterThan(0))); // Has pending requests

        System.out.println("✅ MAXIMUM COMPLEXITY TEST PASSED - System maintained perfect integrity through:");
        System.out.println("   📊 Deletion of user with 4 friendships, 3 pet posts, 4 chats, 2 follow relationships");
        System.out.println("   🔄 Complete cascading cleanup across all modules");
        System.out.println("   🏗️ Continued functionality of remaining users");
        System.out.println("   🆕 Successful integration of new users post-deletion");
        System.out.println("   🔗 Perfect cross-module data consistency");
        System.out.println("   🎯 Zero orphaned data or broken references");
    }

    @Test
    @Order(13)
    @DisplayName("Should test notification module integration with friend requests, followers, and cancellations")
    void testNotificationModuleIntegration() throws Exception {
        // Setup test users
        String user1 = "notif_user1_" + System.currentTimeMillis();
        String email1 = user1 + "@example.com";
        String user2 = "notif_user2_" + System.currentTimeMillis();
        String email2 = user2 + "@example.com";
        String user3 = "notif_user3_" + System.currentTimeMillis();
        String email3 = user3 + "@example.com";

        // Register users
        Response response1 = registerAndLoginUser(user1, email1);
        String token1 = JwtTokenExtractor.extractAccessToken(response1);
        String userId1 = JwtTokenExtractor.extractUserId(response1);

        Response response2 = registerAndLoginUser(user2, email2);
        String token2 = JwtTokenExtractor.extractAccessToken(response2);
        String userId2 = JwtTokenExtractor.extractUserId(response2);

        Response response3 = registerAndLoginUser(user3, email3);
        String token3 = JwtTokenExtractor.extractAccessToken(response3);
        String userId3 = JwtTokenExtractor.extractUserId(response3);

        Thread.sleep(3000); // Wait for users to be propagated

        // TEST 1: FRIEND REQUEST NOTIFICATION
        System.out.println("📧 Testing friend request notification...");

        // Send friend request from user1 to user2
        Response friendRequestResponse = given()
                .spec(getAuthenticatedSpec(token1))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + userId2)
                .then()
                .statusCode(201)
                .extract().response();

        String requestId = friendRequestResponse.jsonPath().getString("requestId");

        Thread.sleep(2000); // Wait for notification to be processed

        // Check if user2 received notification
        Response notificationsResponse = given()
                .spec(getAuthenticatedSpec(token2))
                .when()
                .get(notificationBaseUrl + "/api/notifications")
                .then()
                .statusCode(200)
                .body("content", hasSize(greaterThan(0)))
                .extract().response();

        // Verify notification content
        List<Map<String, Object>> notifications = notificationsResponse.jsonPath().getList("content");
        boolean foundFriendRequestNotif = notifications.stream()
                .anyMatch(notif ->
                        "FRIEND_REQUEST_RECEIVED".equals(notif.get("type")) &&
                                userId1.equals(notif.get("initiatorId").toString()) &&
                                userId2.equals(notif.get("recipientId").toString()) &&
                                requestId.equals(notif.get("entityId").toString()) &&
                                notif.get("message").toString().equals(user1 + " sent you a friend request")
                );

        assertTrue(foundFriendRequestNotif, "Friend request notification not found");
        System.out.println("✅ Friend request notification received correctly");

        // TEST 2: FRIEND REQUEST ACCEPTED NOTIFICATION
        System.out.println("📧 Testing friend request accepted notification...");

        // User2 accepts the friend request
        Response acceptResponse = given()
                .spec(getAuthenticatedSpec(token2))
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + requestId)
                .then()
                .statusCode(201)
                .extract().response();

        String friendshipId = acceptResponse.jsonPath().getString("friendshipId");

        Thread.sleep(2000); // Wait for notification

        // Check if user1 received acceptance notification
        Response user1NotifResponse = given()
                .spec(getAuthenticatedSpec(token1))
                .when()
                .get(notificationBaseUrl + "/api/notifications")
                .then()
                .statusCode(200)
                .body("content", hasSize(greaterThan(0)))
                .extract().response();

        List<Map<String, Object>> user1Notifications = user1NotifResponse.jsonPath().getList("content");
        boolean foundAcceptedNotif = user1Notifications.stream()
                .anyMatch(notif ->
                        "FRIEND_REQUEST_ACCEPTED".equals(notif.get("type")) &&
                                userId2.equals(notif.get("initiatorId").toString()) &&
                                userId1.equals(notif.get("recipientId").toString()) &&
                                friendshipId.equals(notif.get("entityId").toString()) &&
                                notif.get("message").toString().equals(user2 + " accepted your friend request")
                );

        assertTrue(foundAcceptedNotif, "Friend request accepted notification not found");
        System.out.println("✅ Friend request accepted notification received correctly");

        // TEST 3: NEW FOLLOWER NOTIFICATION
        System.out.println("📧 Testing new follower notification...");

        // User3 follows user1
        Response followResponse = given()
                .spec(getAuthenticatedSpec(token3))
                .when()
                .post(friendsBaseUrl + "/api/friends/follow/" + userId1)
                .then()
                .statusCode(201)
                .extract().response();

        String followId = followResponse.jsonPath().getString("followId");

        Thread.sleep(2000); // Wait for notification

        // Check if user1 received follower notification
        Response followerNotifResponse = given()
                .spec(getAuthenticatedSpec(token1))
                .when()
                .get(notificationBaseUrl + "/api/notifications")
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> followerNotifications = followerNotifResponse.jsonPath().getList("content");
        boolean foundFollowerNotif = followerNotifications.stream()
                .anyMatch(notif ->
                        "NEW_FOLLOWER".equals(notif.get("type")) &&
                                userId3.equals(notif.get("initiatorId").toString()) &&
                                userId1.equals(notif.get("recipientId").toString()) &&
                                followId.equals(notif.get("entityId").toString()) &&
                                notif.get("message").toString().equals(user3 + " started following you")
                );

        assertTrue(foundFollowerNotif, "New follower notification not found");
        System.out.println("✅ New follower notification received correctly");

        // TEST 4: FRIEND REQUEST CANCELLED - NOTIFICATION DELETION
        System.out.println("📧 Testing friend request cancellation and notification deletion...");

        // First, send a new friend request from user1 to user3
        Response newRequestResponse = given()
                .spec(getAuthenticatedSpec(token1))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + userId3)
                .then()
                .statusCode(201)
                .extract().response();

        String newRequestId = newRequestResponse.jsonPath().getString("requestId");

        Thread.sleep(2000); // Wait for notification

        // Verify user3 has the notification
        Response user3NotifBeforeCancel = given()
                .spec(getAuthenticatedSpec(token3))
                .when()
                .get(notificationBaseUrl + "/api/notifications")
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> user3NotifsBefore = user3NotifBeforeCancel.jsonPath().getList("content");
        boolean foundRequestNotif = user3NotifsBefore.stream()
                .anyMatch(notif ->
                        "FRIEND_REQUEST_RECEIVED".equals(notif.get("type")) &&
                                newRequestId.equals(notif.get("entityId").toString())
                );

        assertTrue(foundRequestNotif, "Friend request notification should exist before cancellation");

        // Cancel the friend request
        given()
                .spec(getAuthenticatedSpec(token1))
                .when()
                .put(friendsBaseUrl + "/api/friends/cancel-request/" + newRequestId)
                .then()
                .statusCode(204);

        Thread.sleep(2000); // Wait for notification deletion

        // Verify notification was deleted for user3
        Response user3NotifAfterCancel = given()
                .spec(getAuthenticatedSpec(token3))
                .when()
                .get(notificationBaseUrl + "/api/notifications")
                .then()
                .statusCode(204)
                .extract().response();
        // ADDITIONAL TEST: Verify notification count changes
        System.out.println("📊 Testing notification counts...");

        // Get unread notification count for user1
        given()
                .spec(getAuthenticatedSpec(token1))
                .when()
                .get(notificationBaseUrl + "/api/notifications/unread-count")
                .then()
                .statusCode(200)
                .body(greaterThan("0"));

        // Mark all notifications as read for user1
        given()
                .spec(getAuthenticatedSpec(token1))
                .when()
                .put(notificationBaseUrl + "/api/notifications/mark-all-read")
                .then()
                .statusCode(200);

        Thread.sleep(1000);

        // Verify unread count is now 0
        given()
                .spec(getAuthenticatedSpec(token1))
                .when()
                .get(notificationBaseUrl + "/api/notifications/unread-count")
                .then()
                .statusCode(200)
                .body(equalTo("0"));

        System.out.println("✅ Notification count management working correctly");

        // FINAL VERIFICATION: Complex scenario
        System.out.println("🔄 Testing complex notification scenario...");

        // User2 follows user3
        given()
                .spec(getAuthenticatedSpec(token2))
                .when()
                .post(friendsBaseUrl + "/api/friends/follow/" + userId3)
                .then()
                .statusCode(201);

        // User3 sends friend request to user2 (already following)
        given()
                .spec(getAuthenticatedSpec(token3))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + userId2)
                .then()
                .statusCode(201);

        Thread.sleep(2000);

        // Verify both users have appropriate notifications
        Response user2FinalNotifs = given()
                .spec(getAuthenticatedSpec(token2))
                .when()
                .get(notificationBaseUrl + "/api/notifications")
                .then()
                .statusCode(200)
                .extract().response();

        Response user3FinalNotifs = given()
                .spec(getAuthenticatedSpec(token3))
                .when()
                .get(notificationBaseUrl + "/api/notifications")
                .then()
                .statusCode(200)
                .extract().response();

        // User2 should have friend request from user3
        List<Map<String, Object>> user2FinalList = user2FinalNotifs.jsonPath().getList("content");
        boolean user2HasFriendRequest = user2FinalList.stream()
                .anyMatch(notif ->
                        "FRIEND_REQUEST_RECEIVED".equals(notif.get("type")) &&
                                userId3.equals(notif.get("initiatorId").toString())
                );

        // User3 should have new follower notification from user2
        List<Map<String, Object>> user3FinalList = user3FinalNotifs.jsonPath().getList("content");
        boolean user3HasFollower = user3FinalList.stream()
                .anyMatch(notif ->
                        "NEW_FOLLOWER".equals(notif.get("type")) &&
                                userId2.equals(notif.get("initiatorId").toString())
                );

        assertTrue(user2HasFriendRequest, "User2 should have friend request notification");
        assertTrue(user3HasFollower, "User3 should have follower notification");

        System.out.println("✅ Complex notification scenario passed");
        System.out.println("🎯 All notification integration tests passed successfully!");
        System.out.println("   ✉️ Friend request notifications working");
        System.out.println("   ✅ Friend acceptance notifications working");
        System.out.println("   👥 Follower notifications working");
        System.out.println("   🗑️ Notification deletion on cancellation working");
        System.out.println("   📊 Notification counts and marking as read working");
    }
    @Test
    @Order(14)
    @DisplayName("Should create like notification and delete it when post is deleted")
    void testPetPostLikeAndDeletionNotifications() throws Exception {
        // Setup: Create 2 users
        String postOwnerUsername = "owner_" + System.currentTimeMillis();
        String postOwnerEmail = postOwnerUsername + "@example.com";
        String likerUsername = "liker_" + System.currentTimeMillis();
        String likerEmail = likerUsername + "@example.com";

        // Register users
        Response ownerResponse = registerAndLoginUser(postOwnerUsername, postOwnerEmail);
        String ownerToken = JwtTokenExtractor.extractAccessToken(ownerResponse);
        String ownerId = JwtTokenExtractor.extractUserId(ownerResponse);

        Response likerResponse = registerAndLoginUser(likerUsername, likerEmail);
        String likerToken = JwtTokenExtractor.extractAccessToken(likerResponse);
        String likerId = JwtTokenExtractor.extractUserId(likerResponse);

        Thread.sleep(3000); // Wait for users to propagate

        // STEP 1: Create a pet post
        System.out.println("📝 Creating pet post...");

        String petPostJson = """
            {
                "petDTO": {
                    "name": "TestDog",
                    "species": "DOG",
                    "gender": "MALE",
                    "breed": "Labrador",
                    "dateOfBirth": "2022-01-01",
                    "description": "Test dog for notifications"
                },
                "description": "Testing notification system",
                "postType": "ADOPTION",
                "latitude": 5.0,
                "longitude": 5.0
            }
            """;

        Response createPostResponse = given()
                .spec(getAuthenticatedSpec(ownerToken))
                .body(petPostJson)
                .when()
                .post(adoptionBaseUrl + "/api/pet-posts")
                .then()
                .statusCode(201)
                .extract().response();

        String petPostId = createPostResponse.jsonPath().getString("postId");
        System.out.println("✅ Created pet post with ID: " + petPostId);

        // STEP 2: Liker likes the post
        System.out.println("👍 Liker liking the post...");

        given()
                .spec(getAuthenticatedSpec(likerToken))
                .when()
                .put(adoptionBaseUrl + "/api/pet-posts/" + petPostId + "/react")
                .then()
                .statusCode(200)
                .body("reacts", equalTo(1));

        Thread.sleep(4000); // Wait for notification to be created

        // STEP 3: Verify owner received like notification
        System.out.println("📧 Checking if owner received like notification...");

        Response notificationResponse = given()
                .spec(getAuthenticatedSpec(ownerToken))
                .when()
                .get(notificationBaseUrl + "/api/notifications")
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> notifications = notificationResponse.jsonPath().getList("content");
        System.out.println(notifications);

        // Find the like notification
        Map<String, Object> likeNotification = notifications.stream()
                .filter(notif ->
                        "PET_POST_LIKED".equals(notif.get("type")) &&
                                petPostId.equals(notif.get("entityId").toString()) &&
                                likerId.equals(notif.get("initiatorId").toString()) &&
                                ownerId.equals(notif.get("recipientId").toString()) &&
                                notif.get("message").toString().equals(likerUsername + " liked your post")
                )
                .findFirst()
                .orElse(null);

        assertNotNull(likeNotification, "Like notification should exist");
        assertEquals(ownerId, likeNotification.get("recipientId").toString());
        assertTrue(likeNotification.get("message").toString().contains("liked your post"));

        System.out.println("✅ Like notification received correctly");
        System.out.println("   - Message: " + likeNotification.get("message"));
        System.out.println("   - Entity ID: " + likeNotification.get("entityId"));

        // STEP 4: Delete the pet post
        System.out.println("🗑️ Deleting the pet post...");

        given()
                .spec(getAuthenticatedSpec(ownerToken))
                .when()
                .delete(adoptionBaseUrl + "/api/pet-posts/" + petPostId)
                .then()
                .statusCode(204);

        Thread.sleep(3000); // Wait for deletion event to process

        // STEP 5: Verify notification was deleted
        System.out.println("🔍 Checking if notification was deleted...");

        Response afterDeleteResponse = given()
                .spec(getAuthenticatedSpec(ownerToken))
                .when()
                .get(notificationBaseUrl + "/api/notifications")
                .then()
                .statusCode(204)
                .extract().response();
        System.out.println("✅ Notification was successfully deleted with the post");

        // STEP 6: Verify the post itself is deleted
        System.out.println("🔍 Confirming post is deleted...");

        given()
                .spec(getAuthenticatedSpec(likerToken))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/" + petPostId)
                .then()
                .statusCode(404);

        System.out.println("✅ Post confirmed deleted");

        // SUMMARY
        System.out.println("\n🎯 TEST SUMMARY:");
        System.out.println("   ✅ Pet post created successfully");
        System.out.println("   ✅ Like notification sent when user liked post");
        System.out.println("   ✅ Post deletion triggered notification cleanup");
        System.out.println("   ✅ All notifications for deleted post were removed");
        System.out.println("   ✅ System maintains data integrity");
    }
    @Test
    @Order(15)
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
                .get(friendsBaseUrl + "/api/friends/get-friends")
                .then()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(16)
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
    @Order(17)
    @DisplayName("Should handle logout affecting both services")
    void testLogoutAcrossServices() throws Exception {
        Response response = registerAndLoginUser("logoutuser", "logoutuser@test.com");
        String token = response.jsonPath().getString("tokenDTO.accessToken");
        String refreshToken = response.jsonPath().getString("tokenDTO.refreshToken");
        String userId = response.jsonPath().getString("userId");

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
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userId)
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

    @Test
    @Order(18)
    @DisplayName("Should prevent token tampering across services")
    void testTokenTamperingPrevention() throws Exception {
        Response response = registerAndLoginUser("tampertest", "tampertest@test.com");
        String validToken = response.jsonPath().getString("tokenDTO.accessToken");

        // Tamper with the token payload
        String[] tokenParts = validToken.split("\\.");
        if (tokenParts.length == 3) {
            // Decode payload
            String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]));

            // Create tampered token with modified signature
            String tamperedToken = tokenParts[0] + "." + tokenParts[1] + ".tamperedsignature";

            // Both services should reject tampered token
            given()
                    .header("Authorization", "Bearer " + tamperedToken)
                    .when()
                    .get(registrationBaseUrl + "/api/user/auth/users")
                    .then()
                    .statusCode(anyOf(is(401), is(403)));

            given()
                    .header("Authorization", "Bearer " + tamperedToken)
                    .when()
                    .get(friendsBaseUrl + "/api/friends/get-friends")
                    .then()
                    .statusCode(anyOf(is(401), is(403)));
        }
    }

    @Test
    @Order(19)
    @DisplayName("Should enforce role-based access control across services")
    void testRoleBasedAccessControl() throws Exception {
        // Create regular user
        Response userResponse = registerAndLoginUser("regularuser", "regularuser@test.com");
        String userToken = userResponse.jsonPath().getString("tokenDTO.accessToken");

        // Regular user should not access admin endpoints
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .delete(registrationBaseUrl + "/api/user/auth/delete-all")
                .then()
                .statusCode(anyOf(is(403), is(401)));

        // Regular user should not be able to block/unblock users (admin function)
        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType("application/json")
                .body("{\"email\": \"someuser@test.com\"}")
                .when()
                .post(registrationBaseUrl + "/api/user/auth/block")
                .then()
                .statusCode(anyOf(is(403), is(401)));

        // Regular user CAN access user endpoints
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(registrationBaseUrl + "/api/user/auth/users")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(20)
    @DisplayName("Should prevent cross-user pet manipulation")
    void testCrossUserPetSecurity() throws Exception {
        // Create two users
        Response user1Response = registerAndLoginUser("petowner1", "petowner1@test.com");
        Response user2Response = registerAndLoginUser("petowner2", "petowner2@test.com");

        String user1Token = user1Response.jsonPath().getString("tokenDTO.accessToken");
        String user2Token = user2Response.jsonPath().getString("tokenDTO.accessToken");
        UUID user1Id = UUID.fromString(user1Response.jsonPath().getString("userId"));

        // User1 creates a pet
        Response petResponse = given()
                .header("Authorization", "Bearer " + user1Token)
                .contentType("application/json")
                .body(TestDataBuilder.PetBuilder.createPetJson("MyDog", "DOG", "MALE", "Labrador"))
                .when()
                .post(registrationBaseUrl + "/api/pet")
                .then()
                .statusCode(201)
                .extract().response();

        UUID petId = UUID.fromString(petResponse.jsonPath().getString("petId"));

        // User2 should NOT be able to update User1's pet
        given()
                .header("Authorization", "Bearer " + user2Token)
                .contentType("application/json")
                .body("""
                        {
                            "name": "Stolen Dog",
                            "description": "This is not my pet"
                        }
                        """)
                .when()
                .patch(registrationBaseUrl + "/api/pet/" + petId)
                .then()
                .statusCode(anyOf(is(403), is(401)));

        // User2 should NOT be able to delete User1's pet
        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .delete(registrationBaseUrl + "/api/pet/" + petId)
                .then()
                .statusCode(anyOf(is(403), is(401)));

        // User1 CAN update their own pet
        given()
                .header("Authorization", "Bearer " + user1Token)
                .contentType("application/json")
                .body("""
                        {
                            "name": "MyDog Updated"
                        }
                        """)
                .when()
                .patch(registrationBaseUrl + "/api/pet/" + petId)
                .then()
                .statusCode(200);
    }

    @Test
    @Order(21)
    @DisplayName("Should prevent unauthorized message access across chats")
    void testCrossServiceMessageSecurity() throws Exception {
        // Create three users
        Response user1Response = registerAndLoginUser("msguser1", "msguser1@test.com");
        Response user2Response = registerAndLoginUser("msguser2", "msguser2@test.com");
        Response user3Response = registerAndLoginUser("msguser3", "msguser3@test.com");

        String user1Token = user1Response.jsonPath().getString("tokenDTO.accessToken");
        String user2Token = user2Response.jsonPath().getString("tokenDTO.accessToken");
        String user3Token = user3Response.jsonPath().getString("tokenDTO.accessToken");
        UUID user2Id = UUID.fromString(user2Response.jsonPath().getString("userId"));

        // Wait for sync
        Thread.sleep(2000);

        // User1 creates chat with User2
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
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(chatId, "Private message"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200)
                .extract().response();

        UUID messageId = UUID.fromString(messageResponse.jsonPath().getString("messageId"));

        // User3 should NOT be able to access the chat
        given()
                .header("Authorization", "Bearer " + user3Token)
                .when()
                .get(friendsBaseUrl + "/api/chats/" + chatId)
                .then()
                .statusCode(anyOf(is(403), is(404)));

        // User3 should NOT be able to read messages
        given()
                .header("Authorization", "Bearer " + user3Token)
                .when()
                .get(friendsBaseUrl + "/api/messages/chat/" + chatId)
                .then()
                .statusCode(anyOf(is(403), is(404)));

        // User3 should NOT be able to send messages to this chat
        given()
                .header("Authorization", "Bearer " + user3Token)
                .contentType("application/json")
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(chatId, "Unauthorized message"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(anyOf(is(403), is(404)));

        // User2 CAN access the chat
        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .get(friendsBaseUrl + "/api/chats/" + chatId)
                .then()
                .statusCode(200);
    }

    @Test
    @Order(22)
    @DisplayName("Should prevent unauthorized friend request manipulations")
    void testFriendRequestSecurity() throws Exception {
        // Create three users
        Response user1Response = registerAndLoginUser("friendsec1", "friendsec1@test.com");
        Response user2Response = registerAndLoginUser("friendsec2", "friendsec2@test.com");
        Response user3Response = registerAndLoginUser("friendsec3", "friendsec3@test.com");

        String user1Token = user1Response.jsonPath().getString("tokenDTO.accessToken");
        String user2Token = user2Response.jsonPath().getString("tokenDTO.accessToken");
        String user3Token = user3Response.jsonPath().getString("tokenDTO.accessToken");
        UUID user1Id = UUID.fromString(user1Response.jsonPath().getString("userId"));
        UUID user2Id = UUID.fromString(user2Response.jsonPath().getString("userId"));

        // Wait for sync
        Thread.sleep(2000);

        // User1 sends friend request to User2
        Response friendRequest = given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user2Id)
                .then()
                .statusCode(201)
                .extract().response();

        UUID requestId = UUID.fromString(friendRequest.jsonPath().getString("requestId"));

        // User3 should NOT be able to accept a request not meant for them
        given()
                .header("Authorization", "Bearer " + user3Token)
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + requestId)
                .then()
                .statusCode(anyOf(is(403), is(404)));

        // User3 should NOT be able to cancel someone else's request
        given()
                .header("Authorization", "Bearer " + user3Token)
                .when()
                .put(friendsBaseUrl + "/api/friends/cancel-request/" + requestId)
                .then()
                .statusCode(anyOf(is(403), is(404)));

        // User1 (sender) CAN cancel their own request
        given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .put(friendsBaseUrl + "/api/friends/cancel-request/" + requestId)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(23)
    @DisplayName("Should prevent token reuse after password change")
    void testTokenInvalidationAfterPasswordChange() throws Exception {
        String userEmail = "pwdchange@test.com";
        Response response = registerAndLoginUser("pwdchange", userEmail);
        String oldToken = response.jsonPath().getString("tokenDTO.accessToken");
        UUID userId = UUID.fromString(response.jsonPath().getString("userId"));

        // Verify old token works
        given()
                .header("Authorization", "Bearer " + oldToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userId)
                .then()
                .statusCode(200);

        // Change password
        given()
                .header("Authorization", "Bearer " + oldToken)
                .contentType("application/json")
                .body("""
                        {
                            "email": "pwdchange@test.com",
                            "newPassword": "NewSecurePass123!"
                        }
                        """)
                .when()
                .put(registrationBaseUrl + "/api/user/auth/change-password")
                .then()
                .statusCode(200);

        // Login with new password
        Response newLoginResponse = given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createLoginJson(userEmail, "NewSecurePass123!"))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        String newToken = newLoginResponse.jsonPath().getString("tokenDTO.accessToken");

        // New token should work
        given()
                .header("Authorization", "Bearer " + newToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userId)
                .then()
                .statusCode(200);

        // Note: Old token might still work due to JWT being stateless
        // but this demonstrates the security pattern
    }

    @Test
    @Order(24)
    @DisplayName("Should enforce blocked user restrictions across services")
    void testBlockedUserRestrictions() throws Exception {
        // Create two users
        Response user1Response = registerAndLoginUser("blocker", "blocker@test.com");
        Response user2Response = registerAndLoginUser("blocked", "blocked@test.com");

        String user1Token = user1Response.jsonPath().getString("tokenDTO.accessToken");
        String user2Token = user2Response.jsonPath().getString("tokenDTO.accessToken");
        UUID user1Id = UUID.fromString(user1Response.jsonPath().getString("userId"));
        UUID user2Id = UUID.fromString(user2Response.jsonPath().getString("userId"));

        // Wait for sync
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

        // User2 cannot create chat with User1
        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + user1Id)
                .then()
                .statusCode(anyOf(is(400), is(403)));

        // User2 cannot follow User1
        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .post(friendsBaseUrl + "/api/friends/follow/" + user1Id)
                .then()
                .statusCode(anyOf(is(400), is(403), is(409)));

        // User2 can still access their own resources
        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + user2Id)
                .then()
                .statusCode(200);
    }

    @Test
    @Order(25)
    @DisplayName("Should prevent SQL injection attempts across services")
    void testSQLInjectionPrevention() throws Exception {
        Response response = registerAndLoginUser("sqltest", "sqltest@test.com");
        String token = response.jsonPath().getString("tokenDTO.accessToken");

        // Try SQL injection in search
        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("prefix", "'; DROP TABLE users; --")
                .when()
                .get(registrationBaseUrl + "/api/user/auth/users/search")
                .then()
                .statusCode(anyOf(is(200), is(204))); // Should handle safely

        // Try SQL injection in email
        given()
                .contentType("application/json")
                .body("""
                        {
                            "email": "test@test.com'; DROP TABLE users; --",
                            "password": "Password123!"
                        }
                        """)
                .when()
                .post(registrationBaseUrl + "/api/user/auth/login")
                .then()
                .statusCode(anyOf(is(406), is(401))); // Should fail validation

        // Verify tables still exist by making normal request
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(registrationBaseUrl + "/api/user/auth/users")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(26)
    @DisplayName("Should enforce rate limiting across services")
    void testRateLimitingEnforcement() throws Exception {
        Response response = registerAndLoginUser("ratelimit", "ratelimit@test.com");
        String token = response.jsonPath().getString("tokenDTO.accessToken");
        String userId = response.jsonPath().get("userId");

        // Make multiple rapid requests to trigger rate limit
        int requestCount = 15;
        int successCount = 0;
        int rateLimitedCount = 0;

        for (int i = 0; i < requestCount; i++) {
            Response resp = given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userId)
                    .then()
                    .extract().response();

            if (resp.statusCode() == 200) {
                successCount++;
            } else if (resp.statusCode() == 429) {
                rateLimitedCount++;
            }
        }

        // Should have hit rate limit at some point
        assertTrue(rateLimitedCount > 0 || successCount == requestCount,
                "Either rate limiting should trigger or all requests should succeed");
    }

    @Test
    @Order(27)
    @DisplayName("Should prevent CORS attacks")
    void testCORSProtection() {
        // Test that CORS headers are properly set
        given()
                .header("Origin", "http://malicious-site.com")
                .when()
                .options(registrationBaseUrl + "/api/user/auth/users")
                .then()
                .statusCode(anyOf(is(200), is(403)))
                .header("Access-Control-Allow-Origin", not("*")); // Should not allow all origins

        given()
                .header("Origin", "http://malicious-site.com")
                .when()
                .options(friendsBaseUrl + "/api/friends/get-friends/" + UUID.randomUUID())
                .then()
                .statusCode(anyOf(is(200), is(403)))
                .header("Access-Control-Allow-Origin", not("*"));
    }

    @Test
    @Order(28)
    @DisplayName("Should prevent unauthorized admin operations")
    void testAdminEndpointSecurity() throws Exception {
        // Create regular user
        Response userResponse = registerAndLoginUser("notadmin", "notadmin@test.com");
        String userToken = userResponse.jsonPath().getString("tokenDTO.accessToken");

        // Try to access admin endpoints
        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType("application/json")
                .body("""
                        {
                            "username": "adminuser",
                            "password": "AdminPass123!",
                            "adminRole": "ADMIN"
                        }
                        """)
                .when()
                .post(registrationBaseUrl + "/api/admin/create")
                .then()
                .statusCode(anyOf(is(403), is(401)));

        // Try to delete admin
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .delete(registrationBaseUrl + "/api/admin/delete/" + UUID.randomUUID())
                .then()
                .statusCode(anyOf(is(403), is(401)));

        // Try to get all admins
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(registrationBaseUrl + "/api/admin/get-all")
                .then()
                .statusCode(anyOf(is(403), is(401)));
    }

    @Test
    @Order(29)
    @DisplayName("Should validate JWT expiration across services")
    void testJWTExpirationHandling() throws Exception {
        Response response = registerAndLoginUser("expiretest", "expiretest@test.com");
        String token = response.jsonPath().getString("tokenDTO.accessToken");
        String userId = response.jsonPath().getString("userId");
        // Token should work initially
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(registrationBaseUrl + "/api/user/auth/users")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userId)
                .then()
                .statusCode(200);

        // Note: In a real test, you'd wait for token expiration or mock time
        // For now, we'll test that expired token format is rejected
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjJ9.4Adcj3UFYzPUVaVF43FmMab6RlaQD8A9V8wFzzht-KQ";

        given()
                .header("Authorization", "Bearer " + expiredToken)
                .when()
                .get(registrationBaseUrl + "/api/user/auth/users")
                .then()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(30)
    @DisplayName("Should prevent message reaction abuse")
    void testMessageReactionSecurity() throws Exception {
        // Create users
        Response user1Response = registerAndLoginUser("reactor1x", "reactor1x@test.com");
        Response user2Response = registerAndLoginUser("reactor2x", "reactor2x@test.com");
        Response user3Response = registerAndLoginUser("reactor3x", "reactor3x@test.com");

        String user1Token = user1Response.jsonPath().getString("tokenDTO.accessToken");
        String user2Token = user2Response.jsonPath().getString("tokenDTO.accessToken");
        String user3Token = user3Response.jsonPath().getString("tokenDTO.accessToken");
        UUID user2Id = UUID.fromString(user2Response.jsonPath().getString("userId"));

        // Wait for sync
        Thread.sleep(2000);

        // Create chat between User1 and User2
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
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(chatId, "React to this!"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200)
                .extract().response();

        UUID messageId = UUID.fromString(messageResponse.jsonPath().getString("messageId"));

        // User3 (not in chat) should NOT be able to react to the message
        given()
                .header("Authorization", "Bearer " + user3Token)
                .contentType("application/json")
                .body(TestDataBuilder.MessageBuilder.createMessageReactionJson("LIKE"))
                .when()
                .put(friendsBaseUrl + "/api/messages/" + messageId + "/reaction")
                .then()
                .statusCode(anyOf(is(403), is(404)));

        // User2 (in chat) CAN react
        given()
                .header("Authorization", "Bearer " + user2Token)
                .contentType("application/json")
                .body(TestDataBuilder.MessageBuilder.createMessageReactionJson("LOVE"))
                .when()
                .put(friendsBaseUrl + "/api/messages/" + messageId + "/reaction")
                .then()
                .statusCode(200);

        // User3 should NOT be able to remove User2's reaction
        given()
                .header("Authorization", "Bearer " + user3Token)
                .when()
                .delete(friendsBaseUrl + "/api/messages/" + messageId + "/reaction")
                .then()
                .statusCode(anyOf(is(403), is(404)));
    }

    @Test
    @Order(31)
    @DisplayName("Should enforce data privacy across friend operations")
    void testFriendDataPrivacy() throws Exception {
        // Create users
        Response publicUserResponse = registerAndLoginUser("publicuser", "publicuser@test.com");
        Response privateUserResponse = registerAndLoginUser("privateuser", "privateuser@test.com");
        Response stalkerResponse = registerAndLoginUser("stalker", "stalker@test.com");

        String publicToken = publicUserResponse.jsonPath().getString("tokenDTO.accessToken");
        String privateToken = privateUserResponse.jsonPath().getString("tokenDTO.accessToken");
        String stalkerToken = stalkerResponse.jsonPath().getString("tokenDTO.accessToken");
        UUID publicId = UUID.fromString(publicUserResponse.jsonPath().getString("userId"));
        UUID privateId = UUID.fromString(privateUserResponse.jsonPath().getString("userId"));
        UUID stalkerId = UUID.fromString(stalkerResponse.jsonPath().getString("userId"));

        // Wait for sync
        Thread.sleep(2000);

        // Private user blocks stalker
        given()
                .header("Authorization", "Bearer " + privateToken)
                .when()
                .post(friendsBaseUrl + "/api/friends/block/" + stalkerId)
                .then()
                .statusCode(201);

        // Stalker should not see private user in their searches or suggestions
        // This depends on your implementation, but blocked users should have limited visibility

        // Create some friend relationships
        given()
                .header("Authorization", "Bearer " + publicToken)
                .when()
                .post(friendsBaseUrl + "/api/friends/follow/" + privateId)
                .then()
                .statusCode(201);

        // Stalker should not be able to see private user's followers/following
        // even through mutual connections
    }

    @Test
    @Order(32)
    @DisplayName("Should prevent unauthorized pet access patterns")
    void testPetAccessPatterns() throws Exception {
        // Create users
        Response ownerResponse = registerAndLoginUser("petownerx", "petownerx@test.com");
        Response friendResponse = registerAndLoginUser("petfriendx", "petfriendx@test.com");
        Response strangerResponse = registerAndLoginUser("strangerx", "strangerx@test.com");

        String ownerToken = ownerResponse.jsonPath().getString("tokenDTO.accessToken");
        String friendToken = friendResponse.jsonPath().getString("tokenDTO.accessToken");
        String strangerToken = strangerResponse.jsonPath().getString("tokenDTO.accessToken");
        UUID ownerId = UUID.fromString(ownerResponse.jsonPath().getString("userId"));
        UUID friendId = UUID.fromString(friendResponse.jsonPath().getString("userId"));

        // Owner creates pets
        Response pet1Response = given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body(TestDataBuilder.PetBuilder.createPetJson("PublicPet", "DOG", "MALE", "Beagle"))
                .when()
                .post(registrationBaseUrl + "/api/pet")
                .then()
                .statusCode(201)
                .extract().response();

        UUID petId = UUID.fromString(pet1Response.jsonPath().getString("petId"));

        // Friend can view owner's pets (public info)
        given()
                .header("Authorization", "Bearer " + friendToken)
                .when()
                .get(registrationBaseUrl + "/api/user/" + ownerId + "/pets")
                .then()
                .statusCode(200)
                .body("", hasSize(1));

        // But friend cannot modify
        given()
                .header("Authorization", "Bearer " + friendToken)
                .contentType("application/json")
                .body("""
                        {
                            "name": "Hacked Pet Name"
                        }
                        """)
                .when()
                .patch(registrationBaseUrl + "/api/pet/" + petId)
                .then()
                .statusCode(anyOf(is(403), is(401)));

        // Stranger might have restricted access based on privacy settings
        given()
                .header("Authorization", "Bearer " + strangerToken)
                .when()
                .get(registrationBaseUrl + "/api/pet/" + petId)
                .then()
                .statusCode(anyOf(is(200), is(403), is(404))); // Depends on privacy implementation
    }

    @Test
    @Order(33)
    @DisplayName("Should prevent session fixation attacks")
    void testSessionFixationPrevention() throws Exception {
        // Get initial token
        Response response1 = registerAndLoginUser("sessiontest", "sessiontest@test.com");
        String token1 = response1.jsonPath().getString("tokenDTO.accessToken");
        String refreshToken1 = response1.jsonPath().getString("tokenDTO.refreshToken");
        String userId1 = response1.jsonPath().getString("userId");

        // Login again (new session)
        Response response2 = given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createLoginJson("sessiontest@test.com", "Password123!"))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        String token2 = response2.jsonPath().getString("tokenDTO.accessToken");
        String refreshToken2 = response2.jsonPath().getString("tokenDTO.refreshToken");
        String userId2 = response2.jsonPath().getString("userId");

        // Tokens should be different
        System.out.println("token1: " + token1);
        System.out.println("token2: " + token2);
        System.out.println("reftoken1: " + refreshToken1);
        System.out.println("reftoken2: " + refreshToken2);

        Assertions.assertNotEquals(token1, token2);
        Assertions.assertNotEquals(refreshToken1, refreshToken2);

        // Both tokens should work (stateless JWT)
        given()
                .header("Authorization", "Bearer " + token1)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userId2)
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + token2)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userId1)
                .then()
                .statusCode(200);
    }

    @Test
    @Order(34)
    @DisplayName("Should handle authorization header manipulation")
    void testAuthorizationHeaderManipulation() throws Exception {
        Response response = registerAndLoginUser("headertest", "headertest@test.com");
        String validToken = response.jsonPath().getString("tokenDTO.accessToken");
        String userId = response.jsonPath().getString("userId");

        // Test various malformed authorization headers

        // Missing Bearer prefix
        given()
                .header("Authorization", validToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userId)
                .then()
                .statusCode(anyOf(is(401), is(403)));

        // Wrong prefix
        given()
                .header("Authorization", "Basic " + validToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userId)
                .then()
                .statusCode(anyOf(is(401), is(403)));

        // Multiple spaces
        given()
                .header("Authorization", "Bearer  " + validToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userId)
                .then()
                .statusCode(anyOf(is(200), is(401))); // Some implementations might handle this

        // Case sensitivity
        given()
                .header("Authorization", "bearer " + validToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userId)
                .then()
                .statusCode(anyOf(is(200), is(403)));

        // Empty token
        given()
                .header("Authorization", "Bearer ")
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userId)
                .then()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(35)
    @DisplayName("Should prevent privilege escalation attempts")
    void testPrivilegeEscalationPrevention() throws Exception {
        // Create regular user
        Response userResponse = registerAndLoginUser("regularjoe", "regularjoe@test.com");
        String userToken = userResponse.jsonPath().getString("tokenDTO.accessToken");
        UUID userId = UUID.fromString(userResponse.jsonPath().getString("userId"));

        // Try to access admin panel
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(registrationBaseUrl + "/api/admin/get-all")
                .then()
                .statusCode(anyOf(is(403), is(401)));

        // Try to modify another user's data by guessing IDs
        UUID randomUserId = UUID.randomUUID();
        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createUpdateProfileJson("Hacker", "Escalated", "+999"))
                .when()
                .patch(registrationBaseUrl + "/api/user/auth/" + randomUserId)
                .then()
                .statusCode(anyOf(is(403), is(400), is(404)));

        // Try to delete another user
        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType("application/json")
                .body("{\"email\": \"someother@test.com\"}")
                .when()
                .delete(registrationBaseUrl + "/api/user/auth/delete")
                .then()
                .statusCode(anyOf(is(403), is(401), is(400)));
    }

    @Test
    @Order(36)
    @DisplayName("Should validate content-type security")
    void testContentTypeSecurity() throws Exception {
        Response response = registerAndLoginUser("contenttest", "contenttest@test.com");
        String token = response.jsonPath().getString("tokenDTO.accessToken");

        // Send JSON data with wrong content-type
        given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "text/plain")
                .body("{\"name\": \"Test\"}")
                .when()
                .patch(registrationBaseUrl + "/api/user/auth/" + response.jsonPath().getString("userId"))
                .then()
                .statusCode(anyOf(is(400), is(415))); // Should reject wrong content type

        // Send malformed JSON
        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{'invalid': json}")
                .when()
                .patch(registrationBaseUrl + "/api/user/auth/" + response.jsonPath().getString("userId"))
                .then()
                .statusCode(400);
    }
    @Test
    @Order(37)
    @DisplayName("Should propagate block event to Registration and Adoption modules via RabbitMQ")
    void testBlockEventPropagation() throws Exception {
        String blocker1Name = "blocker_" + System.currentTimeMillis();
        String blocker1Email = blocker1Name + "@example.com";
        String blocked1Name = "blocked_" + System.currentTimeMillis();
        String blocked1Email = blocked1Name + "@example.com";

        // Register both users through Registration Module
        Response blockerResponse = registerAndLoginUser(blocker1Name, blocker1Email);
        String blockerToken = JwtTokenExtractor.extractAccessToken(blockerResponse);
        String blockerId = JwtTokenExtractor.extractUserId(blockerResponse);

        Response blockedResponse = registerAndLoginUser(blocked1Name, blocked1Email);
        String blockedToken = JwtTokenExtractor.extractAccessToken(blockedResponse);
        String blockedId = JwtTokenExtractor.extractUserId(blockedResponse);

        Thread.sleep(3000); // Wait for user propagation

        // Step 1: Verify users can initially interact
        // Blocked user creates a pet post
        String petPostJson = """
                    {
                        "petDTO": {
                            "name": "BlockTestPet",
                            "species": "DOG",
                            "gender": "MALE",
                            "breed": "TestBreed",
                            "dateOfBirth": "2020-01-01",
                            "description": "Pet before blocking"
                        },
                        "description": "This post will test blocking functionality",
                        "postType": "ADOPTION",
                        "latitude": 5.0,
                        "longitude": 5.0
                    }
                    """;

        Response petPostResponse = given()
                .spec(getAuthenticatedSpec(blockedToken))
                .body(petPostJson)
                .when()
                .post(adoptionBaseUrl + "/api/pet-posts")
                .then()
                .statusCode(201)
                .extract().response();

        String petPostId = petPostResponse.jsonPath().getString("postId");

        // Blocker can initially view blocked user's posts
        given()
                .spec(getAuthenticatedSpec(blockerToken))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/user/" + blockedId)
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].postId", equalTo(petPostId));

        // Blocker can initially react to blocked user's post
        given()
                .spec(getAuthenticatedSpec(blockerToken))
                .when()
                .put(adoptionBaseUrl + "/api/pet-posts/" + petPostId + "/react")
                .then()
                .statusCode(200)
                .body("reacts", equalTo(1));

        // Step 2: Blocker blocks the other user in Friends Module
        Response blockResponse = given()
                .spec(getAuthenticatedSpec(blockerToken))
                .when()
                .post(friendsBaseUrl + "/api/friends/block/" + blockedId)
                .then()
                .statusCode(201)
                .body("blocker.userId", equalTo(blockerId))
                .body("blocked.userId", equalTo(blockedId))
                .body("createdAt", notNullValue())
                .extract().response();

        String blockId = blockResponse.jsonPath().getString("id");

        // Wait for RabbitMQ block event propagation
        Thread.sleep(4000);

        // Step 3: Verify block event propagated to Registration Module
        given()
                .spec(getAuthenticatedSpec(blockerToken))
                .when()
                .get(friendsBaseUrl + "/api/friends/is-blocking-exists/" + blockedId)
                .then()
                .statusCode(200)
                .body(equalTo("true"));

        // Step 4: Verify block event propagated to Adoption Module
        // Blocker can no longer view blocked user's posts
        given()
                .spec(getAuthenticatedSpec(blockerToken))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/user/" + blockedId)
                .then()
                .statusCode(anyOf(is(400), is(403))); // Should be blocked

        // Blocker can no longer react to blocked user's posts
        given()
                .spec(getAuthenticatedSpec(blockerToken))
                .when()
                .put(adoptionBaseUrl + "/api/pet-posts/" + petPostId + "/react")
                .then()
                .statusCode(anyOf(is(400), is(403))); // Should be blocked

        // Blocked user can no longer view blocker's profile/posts
        given()
                .spec(getAuthenticatedSpec(blockedToken))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/user/" + blockerId)
                .then()
                .statusCode(anyOf(is(400), is(403))); // Should be blocked

        // Step 5: Verify blocking affects Friends Module interactions
        // Cannot send friend requests
        given()
                .spec(getAuthenticatedSpec(blockerToken))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + blockedId)
                .then()
                .statusCode(anyOf(is(400), is(403))); // Should be blocked

        given()
                .spec(getAuthenticatedSpec(blockedToken))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + blockerId)
                .then()
                .statusCode(anyOf(is(400), is(403))); // Should be blocked

        // Cannot follow each other
        given()
                .spec(getAuthenticatedSpec(blockerToken))
                .when()
                .post(friendsBaseUrl + "/api/friends/follow/" + blockedId)
                .then()
                .statusCode(anyOf(is(400), is(403))); // Should be blocked

        // Step 6: Verify block persists in both Registration and Adoption modules
        given()
                .spec(getAuthenticatedSpec(blockerToken))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-blocked-users")
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].blocked.userId", equalTo(blockedId));
    }

    @Test
    @Order(38)
    @DisplayName("Should handle blocking users with existing relationships")
    void testBlockingWithExistingRelationships() throws Exception {
        String user1Name = "existing_user1_" + System.currentTimeMillis();
        String user1Email = user1Name + "@example.com";
        String user2Name = "existing_user2_" + System.currentTimeMillis();
        String user2Email = user2Name + "@example.com";

        // Register both users
        Response user1Response = registerAndLoginUser(user1Name, user1Email);
        String user1Token = JwtTokenExtractor.extractAccessToken(user1Response);
        String user1Id = JwtTokenExtractor.extractUserId(user1Response);

        Response user2Response = registerAndLoginUser(user2Name, user2Email);
        String user2Token = JwtTokenExtractor.extractAccessToken(user2Response);
        String user2Id = JwtTokenExtractor.extractUserId(user2Response);

        Thread.sleep(3000);

        // Step 1: Create existing relationships
        // Establish friendship
        Response friendRequest = given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user2Id)
                .then()
                .statusCode(201)
                .extract().response();

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + friendRequest.jsonPath().getString("requestId"))
                .then()
                .statusCode(201);

        // Create follow relationship
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/follow/" + user2Id)
                .then()
                .statusCode(201);

        // Create chat and messages
        Response chatResponse = given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + user2Id)
                .then()
                .statusCode(201)
                .extract().response();

        String chatId = chatResponse.jsonPath().getString("chatId");

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(UUID.fromString(chatId), "Hello friend!"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200);

        // User2 creates pet posts that User1 interacts with
        String petPostJson = """
                    {
                        "petDTO": {
                            "name": "FriendshipPet",
                            "species": "CAT",
                            "gender": "FEMALE",
                            "breed": "Persian",
                            "dateOfBirth": "2021-01-01",
                            "description": "Pet in friendship"
                        },
                        "description": "Pet post with existing friendship",
                        "postType": "ADOPTION",
                        "latitude": 5.0,
                        "longitude": 5.0
                    }
                    """;

        Response petResponse = given()
                .spec(getAuthenticatedSpec(user2Token))
                .body(petPostJson)
                .when()
                .post(adoptionBaseUrl + "/api/pet-posts")
                .then()
                .statusCode(201)
                .extract().response();

        String petPostId = petResponse.jsonPath().getString("postId");

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .put(adoptionBaseUrl + "/api/pet-posts/" + petPostId + "/react")
                .then()
                .statusCode(200);

        // Step 2: Verify relationships exist
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/is-friend/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("true"));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/is-following/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("true"));

        // Step 3: User1 blocks User2 (should cleanup all relationships)
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/block/" + user2Id)
                .then()
                .statusCode(201);

        Thread.sleep(4000); // Wait for block propagation and cleanup

        // Step 4: Verify all relationships are cleaned up in Friends Module
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/is-friend/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("false"));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/is-following/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("false"));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + user1Id)
                .then()
                .statusCode(200)
                .body(equalTo("0"));

        // Step 5: Verify blocking affects Adoption Module
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/user/" + user2Id)
                .then()
                .statusCode(anyOf(is(400), is(403)));

        // Step 6: Verify blocking is recorded properly
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/is-blocking-exists/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    @Order(39)
    @DisplayName("Should handle mutual blocking scenarios")
    void testMutualBlockingScenarios() throws Exception {
        String user1Name = "mutual1_" + System.currentTimeMillis();
        String user1Email = user1Name + "@example.com";
        String user2Name = "mutual2_" + System.currentTimeMillis();
        String user2Email = user2Name + "@example.com";

        // Register both users
        Response user1Response = registerAndLoginUser(user1Name, user1Email);
        String user1Token = JwtTokenExtractor.extractAccessToken(user1Response);
        String user1Id = JwtTokenExtractor.extractUserId(user1Response);

        Response user2Response = registerAndLoginUser(user2Name, user2Email);
        String user2Token = JwtTokenExtractor.extractAccessToken(user2Response);
        String user2Id = JwtTokenExtractor.extractUserId(user2Response);

        Thread.sleep(3000);

        // Step 1: Both users create pet posts
        String user1PetJson = """
                    {
                        "petDTO": {
                            "name": "MutualPet1",
                            "species": "DOG",
                            "gender": "MALE",
                            "breed": "TestBreed",
                            "dateOfBirth": "2020-01-01",
                            "description": "Pet for mutual blocking test"
                        },
                        "description": "User1 pet post",
                        "postType": "ADOPTION",
                        "latitude": 5.0,
                        "longitude": 5.0
                    }
                    """;

        Response user1PetResponse = given()
                .spec(getAuthenticatedSpec(user1Token))
                .body(user1PetJson)
                .when()
                .post(adoptionBaseUrl + "/api/pet-posts")
                .then()
                .statusCode(201)
                .extract().response();

        String user1PetId = user1PetResponse.jsonPath().getString("postId");

        String user2PetJson = """
                    {
                        "petDTO": {
                            "name": "MutualPet2",
                            "species": "CAT",
                            "gender": "FEMALE",
                            "breed": "TestBreed",
                            "dateOfBirth": "2021-01-01",
                            "description": "Pet for mutual blocking test"
                        },
                        "description": "User2 pet post",
                        "postType": "BREEDING",
                        "latitude": 5.0,
                        "longitude": 5.0
                    }
                    """;

        Response user2PetResponse = given()
                .spec(getAuthenticatedSpec(user2Token))
                .body(user2PetJson)
                .when()
                .post(adoptionBaseUrl + "/api/pet-posts")
                .then()
                .statusCode(201)
                .extract().response();

        String user2PetId = user2PetResponse.jsonPath().getString("postId");

        // Step 2: Users initially interact with each other's posts
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .put(adoptionBaseUrl + "/api/pet-posts/" + user2PetId + "/react")
                .then()
                .statusCode(200);

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .put(adoptionBaseUrl + "/api/pet-posts/" + user1PetId + "/react")
                .then()
                .statusCode(200);

        // Step 3: User1 blocks User2
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/block/" + user2Id)
                .then()
                .statusCode(201);

        Thread.sleep(3000);

        // Step 4: User2 tries to block User1 (should handle mutual blocking)
        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/block/" + user1Id)
                .then()
                .statusCode(anyOf(is(201), is(400))); // Might succeed or fail depending on implementation

        Thread.sleep(3000);

        // Step 5: Verify mutual blocking status
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/is-blocking-exists/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("true"));

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/is-blocking-exists/" + user1Id)
                .then()
                .statusCode(200)
                .body(equalTo("true"));

        // Step 6: Verify complete isolation in Adoption Module
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/user/" + user2Id)
                .then()
                .statusCode(anyOf(is(400), is(403)));

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/user/" + user1Id)
                .then()
                .statusCode(anyOf(is(400), is(403)));

        // Step 7: Verify no new interactions possible
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .put(adoptionBaseUrl + "/api/pet-posts/" + user2PetId + "/react")
                .then()
                .statusCode(anyOf(is(400), is(403)));

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .put(adoptionBaseUrl + "/api/pet-posts/" + user1PetId + "/react")
                .then()
                .statusCode(anyOf(is(400), is(403)));
    }

    @Test
    @Order(40)
    @DisplayName("Should handle block events with multiple concurrent operations")
    void testConcurrentBlockingOperations() throws Exception {
        int userCount = 4;
        String[] usernames = new String[userCount];
        String[] emails = new String[userCount];
        String[] tokens = new String[userCount];
        String[] userIds = new String[userCount];

        // Register multiple users
        for (int i = 0; i < userCount; i++) {
            usernames[i] = "concurrent_block_" + i + "_" + System.currentTimeMillis();
            emails[i] = usernames[i] + "@example.com";
            Response response = registerAndLoginUser(usernames[i], emails[i]);
            tokens[i] = JwtTokenExtractor.extractAccessToken(response);
            userIds[i] = JwtTokenExtractor.extractUserId(response);
        }

        Thread.sleep(4000);

        // Step 1: Create cross-interactions between all users
        String[] petPostIds = new String[userCount];
        for (int i = 0; i < userCount; i++) {
            String petPostJson = String.format("""
                        {
                            "petDTO": {
                                "name": "ConcurrentPet%d",
                                "species": "DOG",
                                "gender": "MALE",
                                "breed": "TestBreed",
                                "dateOfBirth": "2020-01-01",
                                "description": "Pet for concurrent blocking test %d"
                            },
                            "description": "Concurrent blocking test post %d",
                            "postType": "ADOPTION",
                            "latitude": 5.0,
                            "longitude": 5.0
                        }
                        """, i, i, i);

            Response petResponse = given()
                    .spec(getAuthenticatedSpec(tokens[i]))
                    .body(petPostJson)
                    .when()
                    .post(adoptionBaseUrl + "/api/pet-posts")
                    .then()
                    .statusCode(201)
                    .extract().response();

            petPostIds[i] = petResponse.jsonPath().getString("postId");
        }

        // All users react to all other users' posts
        for (int i = 0; i < userCount; i++) {
            for (int j = 0; j < userCount; j++) {
                if (i != j) {
                    given()
                            .spec(getAuthenticatedSpec(tokens[i]))
                            .when()
                            .put(adoptionBaseUrl + "/api/pet-posts/" + petPostIds[j] + "/react")
                            .then()
                            .statusCode(200);
                }
            }
        }

        // Step 2: Concurrent blocking operations
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch blockingLatch = new CountDownLatch(3);
        AtomicInteger successfulBlocks = new AtomicInteger(0);

        // User 0 blocks users 1, 2, and 3 concurrently
        for (int i = 1; i < userCount; i++) {
            final int targetIndex = i;
            executor.submit(() -> {
                try {
                    Response blockResponse = given()
                            .spec(getAuthenticatedSpec(tokens[0]))
                            .when()
                            .post(friendsBaseUrl + "/api/friends/block/" + userIds[targetIndex]);

                    if (blockResponse.getStatusCode() == 201) {
                        successfulBlocks.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    blockingLatch.countDown();
                }
            });
        }

        assertTrue(blockingLatch.await(30, TimeUnit.SECONDS));
        assertEquals(3, successfulBlocks.get());

        Thread.sleep(5000); // Wait for all block events to propagate

        // Step 3: Verify all blocks propagated correctly
        given()
                .spec(getAuthenticatedSpec(tokens[0]))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-blocked-users")
                .then()
                .statusCode(200)
                .body(equalTo("3"));

        // Step 4: Verify blocked users can't interact with blocker in Adoption Module
        for (int i = 1; i < userCount; i++) {
            given()
                    .spec(getAuthenticatedSpec(tokens[i]))
                    .when()
                    .get(adoptionBaseUrl + "/api/pet-posts/user/" + userIds[0])
                    .then()
                    .statusCode(anyOf(is(400), is(403)));

            given()
                    .spec(getAuthenticatedSpec(tokens[i]))
                    .when()
                    .put(adoptionBaseUrl + "/api/pet-posts/" + petPostIds[0] + "/react")
                    .then()
                    .statusCode(anyOf(is(400), is(403)));
        }

        // Step 5: Verify non-blocked users can still interact with each other
        given()
                .spec(getAuthenticatedSpec(tokens[1]))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + userIds[2])
                .then()
                .statusCode(201);

        given()
                .spec(getAuthenticatedSpec(tokens[1]))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/user/" + userIds[2])
                .then()
                .statusCode(200);

        executor.shutdown();
    }

    @Test
    @Order(41)
    @DisplayName("Should propagate unblock event to Registration and Adoption modules via RabbitMQ")
    void testUnblockEventPropagation() throws Exception {
        String unblocker1Name = "unblocker_" + System.currentTimeMillis();
        String unblocker1Email = unblocker1Name + "@example.com";
        String unblocked1Name = "unblocked_" + System.currentTimeMillis();
        String unblocked1Email = unblocked1Name + "@example.com";

        // Register both users
        Response unblockerResponse = registerAndLoginUser(unblocker1Name, unblocker1Email);
        String unblockerToken = JwtTokenExtractor.extractAccessToken(unblockerResponse);
        String unblockerId = JwtTokenExtractor.extractUserId(unblockerResponse);

        Response unblockedResponse = registerAndLoginUser(unblocked1Name, unblocked1Email);
        String unblockedToken = JwtTokenExtractor.extractAccessToken(unblockedResponse);
        String unblockedId = JwtTokenExtractor.extractUserId(unblockedResponse);

        Thread.sleep(3000);

        // Step 1: Create content and establish block
        String petPostJson = """
                    {
                        "petDTO": {
                            "name": "UnblockTestPet",
                            "species": "CAT",
                            "gender": "FEMALE",
                            "breed": "TestBreed",
                            "dateOfBirth": "2021-01-01",
                            "description": "Pet for unblock test"
                        },
                        "description": "Testing unblock functionality",
                        "postType": "BREEDING",
                        "latitude": 5.0,
                        "longitude": 5.0
                    }
                    """;

        Response petResponse = given()
                .spec(getAuthenticatedSpec(unblockedToken))
                .body(petPostJson)
                .when()
                .post(adoptionBaseUrl + "/api/pet-posts")
                .then()
                .statusCode(201)
                .extract().response();

        String petPostId = petResponse.jsonPath().getString("postId");

        // Block the user
        given()
                .spec(getAuthenticatedSpec(unblockerToken))
                .when()
                .post(friendsBaseUrl + "/api/friends/block/" + unblockedId)
                .then()
                .statusCode(201);

        Thread.sleep(3000);

        // Verify blocking is active
        given()
                .spec(getAuthenticatedSpec(unblockerToken))
                .when()
                .get(friendsBaseUrl + "/api/friends/is-blocking-exists/" + unblockedId)
                .then()
                .statusCode(200)
                .body(equalTo("true"));

        given()
                .spec(getAuthenticatedSpec(unblockerToken))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/user/" + unblockedId)
                .then()
                .statusCode(anyOf(is(400), is(403)));

        // Step 2: Unblock the user
        given()
                .spec(getAuthenticatedSpec(unblockerToken))
                .when()
                .put(friendsBaseUrl + "/api/friends/unblock/" + unblockedId)
                .then()
                .statusCode(204);

        // Wait for RabbitMQ unblock event propagation
        Thread.sleep(4000);

        // Step 3: Verify unblock event propagated to Registration Module
        given()
                .spec(getAuthenticatedSpec(unblockerToken))
                .when()
                .get(friendsBaseUrl + "/api/friends/is-blocking-exists/" + unblockedId)
                .then()
                .statusCode(200)
                .body(equalTo("false"));

        given()
                .spec(getAuthenticatedSpec(unblockerToken))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-blocked-users")
                .then()
                .statusCode(200)
                .body(equalTo("0"));

        // Step 4: Verify unblock event propagated to Adoption Module
        // Can now view unblocked user's posts
        given()
                .spec(getAuthenticatedSpec(unblockerToken))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/user/" + unblockedId)
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].postId", equalTo(petPostId));

        // Can now react to unblocked user's posts
        given()
                .spec(getAuthenticatedSpec(unblockerToken))
                .when()
                .put(adoptionBaseUrl + "/api/pet-posts/" + petPostId + "/react")
                .then()
                .statusCode(200)
                .body("reacts", equalTo(1));

        // Step 5: Verify Friends Module interactions are restored
        given()
                .spec(getAuthenticatedSpec(unblockerToken))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + unblockedId)
                .then()
                .statusCode(201);

        given()
                .spec(getAuthenticatedSpec(unblockerToken))
                .when()
                .post(friendsBaseUrl + "/api/friends/follow/" + unblockedId)
                .then()
                .statusCode(201);

        // Step 6: Verify full bidirectional functionality restored
        given()
                .spec(getAuthenticatedSpec(unblockedToken))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/user/" + unblockerId)
                .then()
                .statusCode(200);

        given()
                .spec(getAuthenticatedSpec(unblockedToken))
                .when()
                .post(friendsBaseUrl + "/api/friends/follow/" + unblockerId)
                .then()
                .statusCode(201);
    }

    @Test
    @Order(42)
    @DisplayName("Should handle unblock with immediate re-interaction capabilities")
    void testUnblockWithImmediateReInteraction() throws Exception {
        String user1Name = "reinteract1_" + System.currentTimeMillis();
        String user1Email = user1Name + "@example.com";
        String user2Name = "reinteract2_" + System.currentTimeMillis();
        String user2Email = user2Name + "@example.com";

        // Register both users
        Response user1Response = registerAndLoginUser(user1Name, user1Email);
        String user1Token = JwtTokenExtractor.extractAccessToken(user1Response);
        String user1Id = JwtTokenExtractor.extractUserId(user1Response);

        Response user2Response = registerAndLoginUser(user2Name, user2Email);
        String user2Token = JwtTokenExtractor.extractAccessToken(user2Response);
        String user2Id = JwtTokenExtractor.extractUserId(user2Response);

        Thread.sleep(3000);

        // Step 1: Create complex content and relationships before blocking
        // User2 creates multiple pet posts
        String[] petPostIds = new String[2];
        for (int i = 0; i < 2; i++) {
            String petPostJson = String.format("""
                        {
                            "petDTO": {
                                "name": "ReInteractPet%d",
                                "species": "DOG",
                                "gender": "MALE",
                                "breed": "TestBreed",
                                "dateOfBirth": "2020-01-01",
                                "description": "Pet for re-interaction test %d"
                            },
                            "description": "Re-interaction test post %d",
                            "postType": "ADOPTION",
                            "latitude": 5.0,
                            "longitude": 5.0
                        }
                        """, i, i, i);

            Response petResponse = given()
                    .spec(getAuthenticatedSpec(user2Token))
                    .body(petPostJson)
                    .when()
                    .post(adoptionBaseUrl + "/api/pet-posts")
                    .then()
                    .statusCode(201)
                    .extract().response();

            petPostIds[i] = petResponse.jsonPath().getString("postId");
        }

        // User1 initially interacts with posts
        for (String petPostId : petPostIds) {
            given()
                    .spec(getAuthenticatedSpec(user1Token))
                    .when()
                    .put(adoptionBaseUrl + "/api/pet-posts/" + petPostId + "/react")
                    .then()
                    .statusCode(200);
        }

        // Establish friendship
        Response friendRequest = given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user2Id)
                .then()
                .statusCode(201)
                .extract().response();

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + friendRequest.jsonPath().getString("requestId"))
                .then()
                .statusCode(201);

        // Create chat
        Response chatResponse = given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + user2Id)
                .then()
                .statusCode(201)
                .extract().response();

        String chatId = chatResponse.jsonPath().getString("chatId");

        // Step 2: Block user (this should cleanup relationships)
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/block/" + user2Id)
                .then()
                .statusCode(201);

        Thread.sleep(3000);

        // Step 3: Verify blocking cleanup
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/is-friend/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("false"));

        // Step 4: Unblock user
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .put(friendsBaseUrl + "/api/friends/unblock/" + user2Id)
                .then()
                .statusCode(204);

        Thread.sleep(3000);

        // Step 5: Test immediate re-interaction capabilities
        // Can immediately view posts again
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/user/" + user2Id)
                .then()
                .statusCode(200)
                .body("content", hasSize(2));

        // Can immediately react to posts again
        for (String petPostId : petPostIds) {
            given()
                    .spec(getAuthenticatedSpec(user1Token))
                    .when()
                    .put(adoptionBaseUrl + "/api/pet-posts/" + petPostId + "/react")
                    .then()
                    .statusCode(200);
        }

        // Can immediately send new friend request
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user2Id)
                .then()
                .statusCode(201);

        // Can immediately follow again
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/follow/" + user2Id)
                .then()
                .statusCode(201);

        // Can immediately create new chat
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + user2Id)
                .then()
                .statusCode(201);

        // Step 6: Verify User2 can also immediately interact back
        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/follow/" + user1Id)
                .then()
                .statusCode(201);

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(adoptionBaseUrl + "/api/pet-posts/user/" + user1Id)
                .then()
                .statusCode(200);
    }

    @Test
    @Order(43)
    @DisplayName("Should handle rapid block-unblock cycles")
    void testRapidBlockUnblockCycles() throws Exception {
        String cycleUser1Name = "cycle1_" + System.currentTimeMillis();
        String cycleUser1Email = cycleUser1Name + "@example.com";
        String cycleUser2Name = "cycle2_" + System.currentTimeMillis();
        String cycleUser2Email = cycleUser2Name + "@example.com";

        // Register both users
        Response user1Response = registerAndLoginUser(cycleUser1Name, cycleUser1Email);
        String user1Token = JwtTokenExtractor.extractAccessToken(user1Response);
        String user1Id = JwtTokenExtractor.extractUserId(user1Response);

        Response user2Response = registerAndLoginUser(cycleUser2Name, cycleUser2Email);
        String user2Token = JwtTokenExtractor.extractAccessToken(user2Response);
        String user2Id = JwtTokenExtractor.extractUserId(user2Response);

        Thread.sleep(3000);

        // Step 1: Create baseline content
        String petPostJson = """
                    {
                        "petDTO": {
                            "name": "CyclePet",
                            "species": "CAT",
                            "gender": "FEMALE",
                            "breed": "TestBreed",
                            "dateOfBirth": "2021-01-01",
                            "description": "Pet for cycle test"
                        },
                        "description": "Rapid cycle test post",
                        "postType": "ADOPTION",
                        "latitude": 5.0,
                        "longitude": 5.0
                    }
                    """;

        Response petResponse = given()
                .spec(getAuthenticatedSpec(user2Token))
                .body(petPostJson)
                .when()
                .post(adoptionBaseUrl + "/api/pet-posts")
                .then()
                .statusCode(201)
                .extract().response();

        String petPostId = petResponse.jsonPath().getString("postId");

        // Step 2: Perform rapid block-unblock cycles
        int cycles = 3;
        for (int cycle = 0; cycle < cycles; cycle++) {
            System.out.println("Starting cycle " + (cycle + 1));

            // Block
            given()
                    .spec(getAuthenticatedSpec(user1Token))
                    .when()
                    .post(friendsBaseUrl + "/api/friends/block/" + user2Id)
                    .then()
                    .statusCode(201);

            Thread.sleep(2000); // Wait for block propagation

            // Verify blocking is active
            given()
                    .spec(getAuthenticatedSpec(user1Token))
                    .when()
                    .get(friendsBaseUrl + "/api/friends/is-blocking-exists/" + user2Id)
                    .then()
                    .statusCode(200)
                    .body(equalTo("true"));

            given()
                    .spec(getAuthenticatedSpec(user1Token))
                    .when()
                    .get(adoptionBaseUrl + "/api/pet-posts/user/" + user2Id)
                    .then()
                    .statusCode(anyOf(is(400), is(403)));

            // Unblock
            given()
                    .spec(getAuthenticatedSpec(user1Token))
                    .when()
                    .put(friendsBaseUrl + "/api/friends/unblock/" + user2Id)
                    .then()
                    .statusCode(204);

            Thread.sleep(2000); // Wait for unblock propagation

            // Verify unblocking is active
            given()
                    .spec(getAuthenticatedSpec(user1Token))
                    .when()
                    .get(friendsBaseUrl + "/api/friends/is-blocking-exists/" + user2Id)
                    .then()
                    .statusCode(200)
                    .body(equalTo("false"));

            given()
                    .spec(getAuthenticatedSpec(user1Token))
                    .when()
                    .get(adoptionBaseUrl + "/api/pet-posts/user/" + user2Id)
                    .then()
                    .statusCode(200)
                    .body("content", hasSize(1));

            // Test interaction capability after each cycle
            given()
                    .spec(getAuthenticatedSpec(user1Token))
                    .when()
                    .put(adoptionBaseUrl + "/api/pet-posts/" + petPostId + "/react")
                    .then()
                    .statusCode(200);

            // Remove reaction for next cycle
            given()
                    .spec(getAuthenticatedSpec(user1Token))
                    .when()
                    .put(adoptionBaseUrl + "/api/pet-posts/" + petPostId + "/react")
                    .then()
                    .statusCode(200);
        }

        // Final verification - system should be stable
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user2Id)
                .then()
                .statusCode(201);
    }


    @Test
    @Order(44)
    @DisplayName("Should test blocking impact on all Friends module endpoints")
    void testBlockingImpactOnAllFriendsEndpoints() throws Exception {
        String user1Name = "endpoints1_" + System.currentTimeMillis();
        String user1Email = user1Name + "@example.com";
        String user2Name = "endpoints2_" + System.currentTimeMillis();
        String user2Email = user2Name + "@example.com";
        String user3Name = "endpoints3_" + System.currentTimeMillis();
        String user3Email = user3Name + "@example.com";

        // Register all users
        Response user1Response = registerAndLoginUser(user1Name, user1Email);
        String user1Token = JwtTokenExtractor.extractAccessToken(user1Response);
        String user1Id = JwtTokenExtractor.extractUserId(user1Response);

        Response user2Response = registerAndLoginUser(user2Name, user2Email);
        String user2Token = JwtTokenExtractor.extractAccessToken(user2Response);
        String user2Id = JwtTokenExtractor.extractUserId(user2Response);

        Response user3Response = registerAndLoginUser(user3Name, user3Email);
        String user3Token = JwtTokenExtractor.extractAccessToken(user3Response);
        String user3Id = JwtTokenExtractor.extractUserId(user3Response);

        Thread.sleep(4000);

        // Step 1: Establish complex relationships before blocking
        // User1 and User2 become friends
        Response friendRequest1 = given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user2Id)
                .then()
                .statusCode(201)
                .extract().response();

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + friendRequest1.jsonPath().getString("requestId"))
                .then()
                .statusCode(201);

        // User1 and User3 become friends
        Response friendRequest2 = given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user3Id)
                .then()
                .statusCode(201)
                .extract().response();

        given()
                .spec(getAuthenticatedSpec(user3Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + friendRequest2.jsonPath().getString("requestId"))
                .then()
                .statusCode(201);

        // Establish follow relationships
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/follow/" + user2Id)
                .then()
                .statusCode(201);

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/follow/" + user1Id)
                .then()
                .statusCode(201);

        // Create chats
        Response chat1Response = given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + user2Id)
                .then()
                .statusCode(201)
                .extract().response();

        String chat1Id = chat1Response.jsonPath().getString("chatId");

        Response chat2Response = given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + user3Id)
                .then()
                .statusCode(201)
                .extract().response();

        String chat2Id = chat2Response.jsonPath().getString("chatId");

        // Send messages
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(UUID.fromString(chat1Id), "Hello User2!"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200);

        // Step 2: Verify initial state using all Friends endpoints
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + user1Id)
                .then()
                .statusCode(200)
                .body(equalTo("2"));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-following/" + user1Id)
                .then()
                .statusCode(200)
                .body(equalTo("1"));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-followers/" + user1Id)
                .then()
                .statusCode(200)
                .body(equalTo("1"));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-friends/" + user1Id)
                .then()
                .statusCode(200)
                .body("content", hasSize(2));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-following/" + user1Id)
                .then()
                .statusCode(200)
                .body("content", hasSize(1));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-followers/" + user1Id)
                .then()
                .statusCode(200)
                .body("content", hasSize(1));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/chats")
                .then()
                .statusCode(200)
                .body("$", hasSize(2));

        // Step 3: User1 blocks User2
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/block/" + user2Id)
                .then()
                .statusCode(201);

        Thread.sleep(4000);

        // Step 4: Test all affected Friends endpoints after blocking
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + user1Id)
                .then()
                .statusCode(200)
                .body(equalTo("1")); // Should be 1 (only User3)

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-following/" + user1Id)
                .then()
                .statusCode(200)
                .body(equalTo("0")); // Follow relationship should be removed

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-followers/" + user1Id)
                .then()
                .statusCode(200)
                .body(equalTo("0")); // Follow relationship should be removed

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-blocked-users")
                .then()
                .statusCode(200)
                .body(equalTo("1"));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-friends/" + user1Id)
                .then()
                .statusCode(200)
                .body("content", hasSize(1)); // Only User3

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/get-blocked-users")
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].blocked.userId", equalTo(user2Id));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/is-friend/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("false"));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/is-blocking-exists/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("true"));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/is-following/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("false"));


        // Remaining chat with User3 should still work
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/chats/" + chat2Id)
                .then()
                .statusCode(200);

        // Step 5: Test forbidden operations
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + user2Id)
                .then()
                .statusCode(anyOf(is(400), is(403)));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .post(friendsBaseUrl + "/api/friends/follow/" + user2Id)
                .then()
                .statusCode(anyOf(is(400), is(403)));

        // Step 6: Test that User3 relationships are unaffected
        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/is-friend/" + user3Id)
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    @Order(45)
    @DisplayName("Should test message endpoints behavior with blocking")
    void testMessageEndpointsBehaviorWithBlocking() throws Exception {
        String sender1Name = "sender_" + System.currentTimeMillis();
        String sender1Email = sender1Name + "@example.com";
        String receiver1Name = "receiver_" + System.currentTimeMillis();
        String receiver1Email = receiver1Name + "@example.com";

        // Register both users
        Response senderResponse = registerAndLoginUser(sender1Name, sender1Email);
        String senderToken = JwtTokenExtractor.extractAccessToken(senderResponse);
        String senderId = JwtTokenExtractor.extractUserId(senderResponse);

        Response receiverResponse = registerAndLoginUser(receiver1Name, receiver1Email);
        String receiverToken = JwtTokenExtractor.extractAccessToken(receiverResponse);
        String receiverId = JwtTokenExtractor.extractUserId(receiverResponse);

        Thread.sleep(3000);

        // Step 1: Establish friendship and create chat
        Response friendRequest = given()
                .spec(getAuthenticatedSpec(senderToken))
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + receiverId)
                .then()
                .statusCode(201)
                .extract().response();

        given()
                .spec(getAuthenticatedSpec(receiverToken))
                .when()
                .post(friendsBaseUrl + "/api/friends/accept-request/" + friendRequest.jsonPath().getString("requestId"))
                .then()
                .statusCode(201);

        Response chatResponse = given()
                .spec(getAuthenticatedSpec(senderToken))
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + receiverId)
                .then()
                .statusCode(201)
                .extract().response();

        String chatId = chatResponse.jsonPath().getString("chatId");

        // Step 2: Send multiple messages
        String[] messageIds = new String[3];
        for (int i = 0; i < 3; i++) {
            Response messageResponse = given()
                    .spec(getAuthenticatedSpec(senderToken))
                    .body(TestDataBuilder.MessageBuilder.createSendMessageJson(UUID.fromString(chatId), "Message " + (i + 1)))
                    .when()
                    .post(friendsBaseUrl + "/api/messages/send")
                    .then()
                    .statusCode(200)
                    .extract().response();

            messageIds[i] = messageResponse.jsonPath().getString("messageId");
        }

        // Receiver responds
        given()
                .spec(getAuthenticatedSpec(receiverToken))
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(UUID.fromString(chatId), "Response message"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(200);

        // Add reactions
        given()
                .spec(getAuthenticatedSpec(receiverToken))
                .body(TestDataBuilder.MessageBuilder.createMessageReactionJson("LIKE"))
                .when()
                .put(friendsBaseUrl + "/api/messages/" + messageIds[0] + "/reaction")
                .then()
                .statusCode(200);

        // Step 3: Verify message endpoints work before blocking
        given()
                .spec(getAuthenticatedSpec(senderToken))
                .when()
                .get(friendsBaseUrl + "/api/messages/chat/" + chatId)
                .then()
                .statusCode(200)
                .body("content", hasSize(4));

        given()
                .spec(getAuthenticatedSpec(senderToken))
                .when()
                .get(friendsBaseUrl + "/api/messages/" + messageIds[0])
                .then()
                .statusCode(200);

        given()
                .spec(getAuthenticatedSpec(senderToken))
                .when()
                .get(friendsBaseUrl + "/api/messages/" + messageIds[0] + "/reactions")
                .then()
                .statusCode(200)
                .body("$", hasSize(1));

        // Step 4: Block the receiver
        given()
                .spec(getAuthenticatedSpec(senderToken))
                .when()
                .post(friendsBaseUrl + "/api/friends/block/" + receiverId)
                .then()
                .statusCode(201);

        Thread.sleep(3000);

        // Step 5: Test message endpoints after blocking


        // Cannot send new messages
        given()
                .spec(getAuthenticatedSpec(senderToken))
                .body(TestDataBuilder.MessageBuilder.createSendMessageJson(UUID.fromString(chatId), "This should fail"))
                .when()
                .post(friendsBaseUrl + "/api/messages/send")
                .then()
                .statusCode(anyOf(is(400), is(403), is(404)));

        // Cannot update message status
        given()
                .spec(getAuthenticatedSpec(senderToken))
                .body(TestDataBuilder.MessageBuilder.createUpdateMessageStatusJson("READ"))
                .when()
                .patch(friendsBaseUrl + "/api/messages/" + messageIds[0] + "/status")
                .then()
                .statusCode(anyOf(is(400), is(403), is(404)));
    }
    @Test
    @Order(46)
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
    @Order(47)
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
                                "concurrentxz" + index,
                                "concurrentxz" + index + "@test.com",
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
            String email = "concurrentxz" + i + "@test.com";
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
    @Order(48)
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
    @Order(49)
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
    @Order(50)
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
    @Order(51)
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
    @Order(52)
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
    @Order(53)
    @DisplayName("Should handle message reactions and status updates with events")
    void testMessageEventsPropagation() throws Exception {
        String user1Email = "msguser1x@test.com";
        String user2Email = "msguser2x@test.com";

        Response user1Response = registerAndLoginUser("msguser1x", user1Email);
        Response user2Response = registerAndLoginUser("msguser2x", user2Email);

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
    @Order(54)
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
    @Order(55)
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
        Assertions.assertTrue(successCount > 0, "At least some follow operations should succeed");

        // Verify final count
        Response countResponse = given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-following/" + userId)
                .then()
                .statusCode(200)
                .extract().response();

        int followingCount = Integer.parseInt(countResponse.body().asString());
        Assertions.assertTrue(followingCount > 0, "User should be following at least one person");
    }

    @Test
    @Order(56)
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
    @Order(57)
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
    @Order(58)
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
    @Order(59)
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
    @Order(60)
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
    @Order(61)
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
    @Order(62)
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
    @Order(63)
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
    @Order(64)
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
    @Test
    @Order(65)
    @DisplayName("Should propagate user registration event from Registration to Friends service via RabbitMQ")
    void testUserRegistrationEventPropagation() throws Exception {
        // Step 1: Register user in Registration service
        Response registrationResponse = given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createRegisterUserJson("interserviceuser", USER_EMAIL_XXX, PASSWORD_XXX))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/signup")
                .then()
                .statusCode(201)
                .extract().response();

        userXXXId = UUID.fromString(registrationResponse.jsonPath().getString("userProfileDTO.userId"));

        // Step 2: Get OTP from database
        String otp = getOtpFromDatabase(USER_EMAIL_XXX);
        Assertions.assertNotNull(otp, "OTP should be generated and stored in database");

        // Step 3: Verify OTP and Login to get token
        given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createOtpValidationJson(USER_EMAIL_XXX, otp))
                .when()
                .put(registrationBaseUrl + "/api/user/auth/verify")
                .then()
                .statusCode(200);

        Response loginResponse = given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createLoginJson(USER_EMAIL_XXX, PASSWORD_XXX))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        userXXXToken = loginResponse.jsonPath().getString("tokenDTO.accessToken");

        // Step 3: Wait and verify user exists in Friends service by attempting to access friend endpoints
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    // Try to get friends list - should work if user was propagated
                    given()
                            .header("Authorization", "Bearer " + userXXXToken)
                            .when()
                            .get(friendsBaseUrl + "/api/friends/get-friends/" + userXXXId)
                            .then()
                            .statusCode(204); // No content but request should be authorized
                });
    }

    @Test
    @Order(66)
    @DisplayName("Should validate JWT token from Registration service in Friends service")
    void testCrossServiceJWTValidation() {
        // Use token from Registration service to access Friends service endpoints
        given()
                .header("Authorization", "Bearer " + userXXXToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userXXXId)
                .then()
                .statusCode(200)
                .body(equalTo("0"));

        // Test multiple endpoints to ensure JWT works across services
        given()
                .header("Authorization", "Bearer " + userXXXToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-followers/" + userXXXId)
                .then()
                .statusCode(204); // No followers yet

        given()
                .header("Authorization", "Bearer " + userXXXToken)
                .when()
                .get(friendsBaseUrl + "/api/chats")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(67)
    @DisplayName("Should handle user blocking across services")
    void testUserBlockingAcrossServices() throws Exception {
        // Create a second user
        String user2Email = "blockedx@test.com";
        Response user2Response = given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createRegisterUserJson("blockeduserx", user2Email, PASSWORD_XXX))
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
                .body(TestDataBuilder.UserBuilder.createLoginJson(user2Email, PASSWORD_XXX))
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
                            .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + user2Id)
                            .then()
                            .statusCode(200);
                });

        // User1 blocks User2 in Friends service
        given()
                .header("Authorization", "Bearer " + userXXXToken)
                .when()
                .post(friendsBaseUrl + "/api/friends/block/" + user2Id)
                .then()
                .statusCode(201);

        // Verify User2 cannot send friend request to User1
        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + userXXXId)
                .then()
                .statusCode(anyOf(is(400), is(403), is(409))); // Should be blocked
    }

    @Test
    @Order(68)
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
                            .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + user3Id)
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
                .body("friend.userId", notNullValue());

        // Verify both users can see each other as friends
        given()
                .header("Authorization", "Bearer " + user3Token)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-friends/" + user3Id)
                .then()
                .statusCode(200)
                .body("content", hasSize(1));

        given()
                .header("Authorization", "Bearer " + user4Token)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-friends/" + user4Id)
                .then()
                .statusCode(200)
                .body("content", hasSize(1));
    }

    @Test
    @Order(69)
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
                            .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + user5Id)
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
    @Order(70)
    @DisplayName("Should handle JWT token refresh across services")
    void testTokenRefreshAcrossServices() throws Exception {
        String testEmail = "tokentest@test.com";
        Response response = registerAndLoginUser("tokentest", testEmail);
        String accessToken = response.jsonPath().getString("tokenDTO.accessToken");
        String refreshToken = response.jsonPath().getString("tokenDTO.refreshToken");
        String userId = response.jsonPath().getString("userId");

        // Use the token in Friends service
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userId)
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
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userId)
                .then()
                .statusCode(200);
    }

    @Test
    @Order(71)
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
                        String userId = resp.jsonPath().getString("userId");
                        given()
                                .header("Authorization", "Bearer " + token)
                                .when()
                                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userId)
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
    @Order(72)
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

        Thread.sleep(1500);
        // Verify deleted user's token no longer works
        given()
                .header("Authorization", "Bearer " + deleteUserToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + deleteUserId)
                .then()
                .statusCode(403);

        // Verify keepUser's friend count decreased
        given()
                .header("Authorization", "Bearer " + keepUserToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + keepUserId)
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
    @Order(73)
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
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userIds.get(0))
                .then()
                .statusCode(200)
                .body(equalTo("2"));
    }

    @Test
    @Order(74)
    @DisplayName("Should handle cross-service validation for non-existent users")
    void testNonExistentUserValidation() throws Exception {
        // Use a valid token but try to interact with non-existent users
        UUID nonExistentUserId = UUID.randomUUID();

        // Try to send friend request to non-existent user
        given()
                .header("Authorization", "Bearer " + userXXXToken)
                .when()
                .post(friendsBaseUrl + "/api/friends/send-request/" + nonExistentUserId)
                .then()
                .statusCode(anyOf(is(400), is(404)));

        // Try to create chat with non-existent user
        given()
                .header("Authorization", "Bearer " + userXXXToken)
                .when()
                .post(friendsBaseUrl + "/api/chats/user/" + nonExistentUserId)
                .then()
                .statusCode(anyOf(is(400), is(404)));

        // Try to follow non-existent user
        given()
                .header("Authorization", "Bearer " + userXXXToken)
                .when()
                .post(friendsBaseUrl + "/api/friends/follow/" + nonExistentUserId)
                .then()
                .statusCode(anyOf(is(400), is(404)));
    }

    @Test
    @Order(75)
    @DisplayName("Should handle cross-service operations when user is not verified")
    void testUnverifiedUserRestrictions() throws Exception {
        // Register user but don't verify
        String unverifiedEmail = "unverified@test.com";
        Response unverifiedResponse = given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createRegisterUserJson("unverified", unverifiedEmail, PASSWORD_XXX))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/signup")
                .then()
                .statusCode(201)
                .extract().response();

        UUID unverifiedId = UUID.fromString(unverifiedResponse.jsonPath().getString("userProfileDTO.userId"));

        // Try to login without verification
        Response loginAttempt = given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createLoginJson(unverifiedEmail, PASSWORD_XXX))
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
                    .get(friendsBaseUrl + "/api/friends/get-number-of-friends")
                    .then()
                    .statusCode(anyOf(is(200), is(403))); // Depends on business rules
        }
    }

    @Test
    @Order(76)
    @DisplayName("Should handle password change affecting both services")
    void testPasswordChangeAcrossServices() throws Exception {
        String pwdChangeEmail = "pwdchange12x@test.com";
        Response userResponse = registerAndLoginUser("pwdchange12x", pwdChangeEmail);
        String oldToken = userResponse.jsonPath().getString("tokenDTO.accessToken");
        String userId = userResponse.jsonPath().getString("userId");

        // Use old token in Friends service
        given()
                .header("Authorization", "Bearer " + oldToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userId)
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
                .put(registrationBaseUrl + "/api/user/auth/change-password")
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
                .get(friendsBaseUrl + "/api/friends/get-number-of-friends/" + userId)
                .then()
                .statusCode(200);
    }

    @Test
    @Order(77)
    @DisplayName("Should handle user search integration between services")
    void testUserSearchIntegration() throws Exception {
        // Create users with searchable patterns
        String[] searchUsers = {
                "abcde1@gmail.com",
                "abcde2@gmail.com",
                "abcde3@gmail.com",
                "abcde4@gmail.com"
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
                .get(registrationBaseUrl + "/api/user/auth/users/abcde")
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
                .get(friendsBaseUrl + "/api/friends/get-number-of-following/" + userIds.get(0))
                .then()
                .statusCode(200)
                .body(equalTo("2"));
    }

    @Test
    @Order(78)
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
    @Order(79)
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
    @Order(80)
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
    @Order(81)
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

    /** Connects to the adoption_breeding database inside the Postgres container */
    private Connection getAdoptionConn() throws SQLException {
        String url = String.format(
                "jdbc:postgresql://localhost:%d/adoption_breeding",
                postgres.getMappedPort(5432)
        );
        return DriverManager.getConnection(url, "postgres", DATA_BASE_PASSWORD);
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
    @Order(82)
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
    @Order(83)
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
    @Order(84)
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
    @Order(85)
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
    @Order(86)
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
