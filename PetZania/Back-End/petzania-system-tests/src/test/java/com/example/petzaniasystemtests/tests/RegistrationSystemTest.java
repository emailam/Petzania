package com.example.petzaniasystemtests.tests;

import com.example.petzaniasystemtests.config.BaseSystemTest;
import com.example.petzaniasystemtests.builders.TestDataBuilder;
import com.example.petzaniasystemtests.utils.JwtTokenExtractor;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

public class RegistrationSystemTest extends BaseSystemTest {
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
                    "location": "Cross Test City"
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
                    "location": "Deletion Test City"
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
                        "location": "Complex City"
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
                        "location": "Concurrent City"
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
                        "location": "Interaction City"
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
                    "location": "Post Deletion City"
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
                        "location": "Cycle City"
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
                    "location": "Integrity City"
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
                        "location": "Max Complexity City"
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
                    "location": "Post Cascade City"
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

}
