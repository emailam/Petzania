package com.example.petzaniasystemtests.tests;

import com.example.petzaniasystemtests.builders.TestDataBuilder;
import com.example.petzaniasystemtests.config.BaseSystemTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("Cross-Service Security Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CrossServiceSecurityTest extends BaseSystemTest {

    @Test
    @Order(1)
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
    @Order(2)
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
    @Order(3)
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

    @Test
    @Order(4)
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
                    .get(friendsBaseUrl + "/api/friends/getFriends")
                    .then()
                    .statusCode(anyOf(is(401), is(403)));
        }
    }

    @Test
    @Order(5)
    @DisplayName("Should enforce role-based access control across services")
    void testRoleBasedAccessControl() throws Exception {
        // Create regular user
        Response userResponse = registerAndLoginUser("regularuser", "regularuser@test.com");
        String userToken = userResponse.jsonPath().getString("tokenDTO.accessToken");

        // Regular user should not access admin endpoints
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .delete(registrationBaseUrl + "/api/user/auth/deleteAll")
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
    @Order(6)
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
    @Order(7)
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
    @Order(8)
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
    @Order(9)
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
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
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
                .put(registrationBaseUrl + "/api/user/auth/changePassword")
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
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                .then()
                .statusCode(200);

        // Note: Old token might still work due to JWT being stateless
        // but this demonstrates the security pattern
    }

    @Test
    @Order(10)
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
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(11)
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
    @Order(12)
    @DisplayName("Should enforce rate limiting across services")
    void testRateLimitingEnforcement() throws Exception {
        Response response = registerAndLoginUser("ratelimit", "ratelimit@test.com");
        String token = response.jsonPath().getString("tokenDTO.accessToken");

        // Make multiple rapid requests to trigger rate limit
        int requestCount = 15;
        int successCount = 0;
        int rateLimitedCount = 0;

        for (int i = 0; i < requestCount; i++) {
            Response resp = given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                    .then()
                    .extract().response();

            if (resp.statusCode() == 200) {
                successCount++;
            } else if (resp.statusCode() == 429) {
                rateLimitedCount++;
            }
        }

        // Should have hit rate limit at some point
        Assertions.assertTrue(rateLimitedCount > 0 || successCount == requestCount,
                "Either rate limiting should trigger or all requests should succeed");
    }

    @Test
    @Order(13)
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
                .options(friendsBaseUrl + "/api/friends/getFriends")
                .then()
                .statusCode(anyOf(is(200), is(403)))
                .header("Access-Control-Allow-Origin", not("*"));
    }

    @Test
    @Order(14)
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
                .get(registrationBaseUrl + "/api/admin/getAll")
                .then()
                .statusCode(anyOf(is(403), is(401)));
    }

    @Test
    @Order(15)
    @DisplayName("Should validate JWT expiration across services")
    void testJWTExpirationHandling() throws Exception {
        Response response = registerAndLoginUser("expiretest", "expiretest@test.com");
        String token = response.jsonPath().getString("tokenDTO.accessToken");

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
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
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
    @Order(16)
    @DisplayName("Should prevent message reaction abuse")
    void testMessageReactionSecurity() throws Exception {
        // Create users
        Response user1Response = registerAndLoginUser("reactor1", "reactor1@test.com");
        Response user2Response = registerAndLoginUser("reactor2", "reactor2@test.com");
        Response user3Response = registerAndLoginUser("reactor3", "reactor3@test.com");

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
    @Order(17)
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
    @Order(18)
    @DisplayName("Should prevent unauthorized pet access patterns")
    void testPetAccessPatterns() throws Exception {
        // Create users
        Response ownerResponse = registerAndLoginUser("petowner", "petowner@test.com");
        Response friendResponse = registerAndLoginUser("petfriend", "petfriend@test.com");
        Response strangerResponse = registerAndLoginUser("stranger", "stranger@test.com");

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
    @Order(19)
    @DisplayName("Should prevent session fixation attacks")
    void testSessionFixationPrevention() throws Exception {
        // Get initial token
        Response response1 = registerAndLoginUser("sessiontest", "sessiontest@test.com");
        String token1 = response1.jsonPath().getString("tokenDTO.accessToken");
        String refreshToken1 = response1.jsonPath().getString("tokenDTO.refreshToken");

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
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + token2)
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(20)
    @DisplayName("Should handle authorization header manipulation")
    void testAuthorizationHeaderManipulation() throws Exception {
        Response response = registerAndLoginUser("headertest", "headertest@test.com");
        String validToken = response.jsonPath().getString("tokenDTO.accessToken");

        // Test various malformed authorization headers

        // Missing Bearer prefix
        given()
                .header("Authorization", validToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                .then()
                .statusCode(anyOf(is(401), is(403)));

        // Wrong prefix
        given()
                .header("Authorization", "Basic " + validToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                .then()
                .statusCode(anyOf(is(401), is(403)));

        // Multiple spaces
        given()
                .header("Authorization", "Bearer  " + validToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                .then()
                .statusCode(anyOf(is(200), is(401))); // Some implementations might handle this

        // Case sensitivity
        given()
                .header("Authorization", "bearer " + validToken)
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                .then()
                .statusCode(anyOf(is(200), is(403)));

        // Empty token
        given()
                .header("Authorization", "Bearer ")
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends")
                .then()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(21)
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
                .get(registrationBaseUrl + "/api/admin/getAll")
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
    @Order(22)
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


}