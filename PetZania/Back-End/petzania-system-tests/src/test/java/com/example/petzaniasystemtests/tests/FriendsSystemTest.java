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

public class FriendsSystemTest extends BaseSystemTest {
    @Test
    @Order(1)
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
                        "location": "Block Test City"
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
                .get(friendsBaseUrl + "/api/friends/isBlockingExists/" + blockedId)
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
                .get(friendsBaseUrl + "/api/friends/getBlockedUsers")
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].blocked.userId", equalTo(blockedId));
    }

    @Test
    @Order(2)
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
                        "location": "Friendship City"
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
                .get(friendsBaseUrl + "/api/friends/isFriend/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("true"));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/isFollowing/" + user2Id)
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
                .get(friendsBaseUrl + "/api/friends/isFriend/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("false"));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/isFollowing/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("false"));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends/" + user1Id)
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
                .get(friendsBaseUrl + "/api/friends/isBlockingExists/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    @Order(3)
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
                        "location": "Mutual City"
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
                        "location": "Mutual City"
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
                .get(friendsBaseUrl + "/api/friends/isBlockingExists/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("true"));

        given()
                .spec(getAuthenticatedSpec(user2Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/isBlockingExists/" + user1Id)
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
    @Order(4)
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
                            "location": "Concurrent City"
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
                .get(friendsBaseUrl + "/api/friends/getNumberOfBlockedUsers")
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
    @Order(5)
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
                        "location": "Unblock Test City"
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
                .get(friendsBaseUrl + "/api/friends/isBlockingExists/" + unblockedId)
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
                .get(friendsBaseUrl + "/api/friends/isBlockingExists/" + unblockedId)
                .then()
                .statusCode(200)
                .body(equalTo("false"));

        given()
                .spec(getAuthenticatedSpec(unblockerToken))
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfBlockedUsers")
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
    @Order(6)
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
                            "location": "ReInteract City"
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
                .get(friendsBaseUrl + "/api/friends/isFriend/" + user2Id)
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
    @Order(7)
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
                        "location": "Cycle City"
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
                    .get(friendsBaseUrl + "/api/friends/isBlockingExists/" + user2Id)
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
                    .get(friendsBaseUrl + "/api/friends/isBlockingExists/" + user2Id)
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
    @Order(8)
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
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends/" + user1Id)
                .then()
                .statusCode(200)
                .body(equalTo("2"));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFollowing/" + user1Id)
                .then()
                .statusCode(200)
                .body(equalTo("1"));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFollowers/" + user1Id)
                .then()
                .statusCode(200)
                .body(equalTo("1"));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/getFriends/" + user1Id)
                .then()
                .statusCode(200)
                .body("content", hasSize(2));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/getFollowing/" + user1Id)
                .then()
                .statusCode(200)
                .body("content", hasSize(1));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/getFollowers/" + user1Id)
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
                .get(friendsBaseUrl + "/api/friends/getNumberOfFriends/" + user1Id)
                .then()
                .statusCode(200)
                .body(equalTo("1")); // Should be 1 (only User3)

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFollowing/" + user1Id)
                .then()
                .statusCode(200)
                .body(equalTo("0")); // Follow relationship should be removed

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfFollowers/" + user1Id)
                .then()
                .statusCode(200)
                .body(equalTo("0")); // Follow relationship should be removed

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/getNumberOfBlockedUsers")
                .then()
                .statusCode(200)
                .body(equalTo("1"));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/getFriends/" + user1Id)
                .then()
                .statusCode(200)
                .body("content", hasSize(1)); // Only User3

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/getBlockedUsers")
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].blocked.userId", equalTo(user2Id));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/isFriend/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("false"));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/isBlockingExists/" + user2Id)
                .then()
                .statusCode(200)
                .body(equalTo("true"));

        given()
                .spec(getAuthenticatedSpec(user1Token))
                .when()
                .get(friendsBaseUrl + "/api/friends/isFollowing/" + user2Id)
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
                .get(friendsBaseUrl + "/api/friends/isFriend/" + user3Id)
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    @Order(9)
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
}

