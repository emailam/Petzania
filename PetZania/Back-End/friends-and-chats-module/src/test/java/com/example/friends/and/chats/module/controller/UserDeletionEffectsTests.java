package com.example.friends.and.chats.module.controller;

import com.example.friends.and.chats.module.model.entity.*;
import com.example.friends.and.chats.module.model.enumeration.MessageReact;
import com.example.friends.and.chats.module.model.enumeration.MessageStatus;
import com.example.friends.and.chats.module.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
@Transactional
public class UserDeletionEffectsTests {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private UserChatRepository userChatRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageReactionRepository messageReactionRepository;

    @PersistenceContext
    private EntityManager entityManager; // Add this field

    @BeforeEach
    void cleanDatabase() {
        messageReactionRepository.deleteAll();
        messageRepository.deleteAll();
        userChatRepository.deleteAll();
        chatRepository.deleteAll();
        friendRequestRepository.deleteAll();
        friendshipRepository.deleteAll();
        blockRepository.deleteAll();
        followRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testUserDeletion_ShouldCascadeDeleteAllFriendships() {
        // Given: Users with multiple friendships
        String timestamp = String.valueOf(System.currentTimeMillis());
        User userToDelete = createUser("deleteUser_" + timestamp);
        User friend1 = createUser("friend1_" + timestamp);
        User friend2 = createUser("friend2_" + timestamp);
        User unrelatedUser1 = createUser("unrelated1_" + timestamp);
        User unrelatedUser2 = createUser("unrelated2_" + timestamp);

        // Create friendships involving userToDelete
        Friendship friendship1 = createFriendship(userToDelete, friend1);
        Friendship friendship2 = createFriendship(userToDelete, friend2);

        // Create unrelated friendship
        Friendship unrelatedFriendship = createFriendship(unrelatedUser1, unrelatedUser2);
        Friendship unrelatedFriendship2 = createFriendship(unrelatedUser1, friend1);
        Friendship unrelatedFriendship3 = createFriendship(unrelatedUser1, friend2);
        Friendship unrelatedFriendship4 = createFriendship(unrelatedUser2, friend1);
        Friendship unrelatedFriendship5 = createFriendship(unrelatedUser2, friend2);


        // Delete the user
        entityManager.flush();
        entityManager.clear();
        userRepository.deleteById(userToDelete.getUserId());
        entityManager.flush();


        // Then: Only friendships involving deleted user should be removed
        assertFalse(userRepository.existsById(userToDelete.getUserId()));
        assertFalse(friendshipRepository.existsById(friendship1.getId()));
        assertFalse(friendshipRepository.existsById(friendship2.getId()));

        // Unrelated entities should remain
        assertTrue(friendshipRepository.existsById(unrelatedFriendship.getId()));
        assertTrue(friendshipRepository.existsById(unrelatedFriendship2.getId()));
        assertTrue(friendshipRepository.existsById(unrelatedFriendship3.getId()));
        assertTrue(friendshipRepository.existsById(unrelatedFriendship4.getId()));
        assertTrue(friendshipRepository.existsById(unrelatedFriendship5.getId()));
        assertTrue(userRepository.existsById(friend1.getUserId()));
        assertTrue(userRepository.existsById(friend2.getUserId()));
        assertTrue(userRepository.existsById(unrelatedUser1.getUserId()));
        assertTrue(userRepository.existsById(unrelatedUser2.getUserId()));
    }

    @Test
    @Transactional
    void testUserDeletion_ShouldCascadeDeleteAllFollowRelationships() {
        // Given: User with follow relationships (both as follower and followed)
        String timestamp = String.valueOf(System.currentTimeMillis());
        User userToDelete = createUser("deleteUser_" + timestamp);
        User followedByUser = createUser("followedByUser_" + timestamp);
        User followerOfUser = createUser("followerOfUser_" + timestamp);
        User unrelatedUser1 = createUser("unrelated1_" + timestamp);
        User unrelatedUser2 = createUser("unrelated2_" + timestamp);

        // User follows someone and is followed by someone
        Follow userFollows = createFollow(userToDelete, followedByUser);
        Follow someoneFollowsUser = createFollow(followerOfUser, userToDelete);

        // Unrelated follow
        Follow unrelatedFollow = createFollow(unrelatedUser1, unrelatedUser2);
        Follow unrelatedFollow2 = createFollow(unrelatedUser1, followerOfUser);
        Follow unrelatedFollow3 = createFollow(unrelatedUser1, followedByUser);
        Follow unrelatedFollow4 = createFollow(unrelatedUser2, followerOfUser);
        Follow unrelatedFollow5 = createFollow(unrelatedUser2, followedByUser);

        // Delete the user
        entityManager.flush();
        entityManager.clear();
        userRepository.deleteById(userToDelete.getUserId());
        entityManager.flush();

        // Then: All follows involving deleted user should be removed
        assertFalse(userRepository.existsById(userToDelete.getUserId()));
        assertFalse(followRepository.existsById(userFollows.getId()));
        assertFalse(followRepository.existsById(someoneFollowsUser.getId()));

        // Unrelated entities should remain
        assertTrue(followRepository.existsById(unrelatedFollow.getId()));
        assertTrue(followRepository.existsById(unrelatedFollow2.getId()));
        assertTrue(followRepository.existsById(unrelatedFollow3.getId()));
        assertTrue(followRepository.existsById(unrelatedFollow4.getId()));
        assertTrue(followRepository.existsById(unrelatedFollow5.getId()));
        assertTrue(userRepository.existsById(followedByUser.getUserId()));
        assertTrue(userRepository.existsById(followerOfUser.getUserId()));
        assertTrue(userRepository.existsById(unrelatedUser1.getUserId()));
        assertTrue(userRepository.existsById(unrelatedUser2.getUserId()));
    }

    @Test
    @Transactional
    void testUserDeletion_ShouldCascadeDeleteAllBlockRelationships() {
        // Given: User with block relationships (both as blocker and blocked)
        String timestamp = String.valueOf(System.currentTimeMillis());
        User userToDelete = createUser("deleteUser_" + timestamp);
        User blockedByUser = createUser("blockedByUser_" + timestamp);
        User blockerOfUser = createUser("blockerOfUser_" + timestamp);
        User unrelatedUser1 = createUser("unrelated1_" + timestamp);
        User unrelatedUser2 = createUser("unrelated2_" + timestamp);

        // User blocks someone and is blocked by someone
        Block userBlocks = createBlock(userToDelete, blockedByUser);
        Block someoneBlocksUser = createBlock(blockerOfUser, userToDelete);

        // Unrelated block
        Block unrelatedBlock = createBlock(unrelatedUser1, unrelatedUser2);
        Block unrelatedBlock2 = createBlock(unrelatedUser1, blockerOfUser);
        Block unrelatedBlock3 = createBlock(unrelatedUser1, blockedByUser);
        Block unrelatedBlock4 = createBlock(unrelatedUser2, blockerOfUser);
        Block unrelatedBlock5 = createBlock(unrelatedUser2, blockedByUser);


        // Delete the user
        entityManager.flush();
        entityManager.clear();
        userRepository.deleteById(userToDelete.getUserId());
        entityManager.flush();


        // Then: All blocks involving deleted user should be removed
        assertFalse(userRepository.existsById(userToDelete.getUserId()));
        assertFalse(blockRepository.existsById(userBlocks.getId()));
        assertFalse(blockRepository.existsById(someoneBlocksUser.getId()));

        // Unrelated entities should remain
        assertTrue(blockRepository.existsById(unrelatedBlock.getId()));
        assertTrue(blockRepository.existsById(unrelatedBlock2.getId()));
        assertTrue(blockRepository.existsById(unrelatedBlock3.getId()));
        assertTrue(blockRepository.existsById(unrelatedBlock4.getId()));
        assertTrue(blockRepository.existsById(unrelatedBlock5.getId()));
        assertTrue(userRepository.existsById(unrelatedUser1.getUserId()));
        assertTrue(userRepository.existsById(unrelatedUser2.getUserId()));
        assertTrue(userRepository.existsById(blockedByUser.getUserId()));
        assertTrue(userRepository.existsById(blockerOfUser.getUserId()));
    }

    @Test
    @Transactional
    void testUserDeletion_ShouldCascadeDeleteAllFriendRequests() {
        // Given: User with sent and received friend requests
        String timestamp = String.valueOf(System.currentTimeMillis());
        User userToDelete = createUser("deleteUser_" + timestamp);
        User receiver = createUser("receiver_" + timestamp);
        User sender = createUser("sender_" + timestamp);
        User unrelatedUser1 = createUser("unrelated1_" + timestamp);
        User unrelatedUser2 = createUser("unrelated2_" + timestamp);

        // User sends and receives requests
        FriendRequest sentRequest = createFriendRequest(userToDelete, receiver);
        FriendRequest receivedRequest = createFriendRequest(sender, userToDelete);

        // Unrelated request
        FriendRequest unrelatedRequest = createFriendRequest(unrelatedUser1, unrelatedUser2);
        FriendRequest unrelatedRequest2 = createFriendRequest(unrelatedUser1, receiver);
        FriendRequest unrelatedRequest3 = createFriendRequest(unrelatedUser1, sender);
        FriendRequest unrelatedRequest4 = createFriendRequest(unrelatedUser2, receiver);
        FriendRequest unrelatedRequest5 = createFriendRequest(unrelatedUser2, sender);

        // Delete the user
        entityManager.flush();
        entityManager.clear();
        userRepository.deleteById(userToDelete.getUserId());
        entityManager.flush();

        // Then: All friend requests involving deleted user should be removed
        assertFalse(userRepository.existsById(userToDelete.getUserId()));
        assertFalse(friendRequestRepository.existsById(sentRequest.getId()));
        assertFalse(friendRequestRepository.existsById(receivedRequest.getId()));

        // Unrelated entities should remain
        assertTrue(friendRequestRepository.existsById(unrelatedRequest.getId()));
        assertTrue(friendRequestRepository.existsById(unrelatedRequest2.getId()));
        assertTrue(friendRequestRepository.existsById(unrelatedRequest3.getId()));
        assertTrue(friendRequestRepository.existsById(unrelatedRequest4.getId()));
        assertTrue(friendRequestRepository.existsById(unrelatedRequest5.getId()));
        assertTrue(userRepository.existsById(receiver.getUserId()));
        assertTrue(userRepository.existsById(sender.getUserId()));
        assertTrue(userRepository.existsById(unrelatedUser1.getUserId()));
        assertTrue(userRepository.existsById(unrelatedUser2.getUserId()));
    }

    @Test
    @Transactional
    void testUserDeletion_ShouldHandleChatDeletionCorrectly() {
        // Given: User in multiple chats
        String timestamp = String.valueOf(System.currentTimeMillis());
        User userToDelete = createUser("deleteUser_" + timestamp);
        User chatPartner1 = createUser("partner1_" + timestamp);
        User chatPartner2 = createUser("partner2_" + timestamp);
        User unrelatedUser1 = createUser("unrelated1_" + timestamp);
        User unrelatedUser2 = createUser("unrelated2_" + timestamp);

        // Create chats
        Chat chat1 = createChat(userToDelete, chatPartner1);
        Chat chat2 = createChat(userToDelete, chatPartner2);
        Chat unrelatedChat = createChat(unrelatedUser1, unrelatedUser2);
        Chat unrelatedChat2 = createChat(unrelatedUser1, chatPartner1);
        Chat unrelatedChat3 = createChat(unrelatedUser1, chatPartner2);

        // Create UserChats
        UserChat userChat1 = createUserChat(chat1, userToDelete);
        UserChat partnerChat1 = createUserChat(chat1, chatPartner1);
        UserChat userChat2 = createUserChat(chat2, userToDelete);
        UserChat partnerChat2 = createUserChat(chat2, chatPartner2);
        UserChat unrelatedUserChat1 = createUserChat(unrelatedChat, unrelatedUser1);
        UserChat unrelatedUserChat2 = createUserChat(unrelatedChat, unrelatedUser2);
        UserChat unrelatedUserChat3 = createUserChat(unrelatedChat2, unrelatedUser1);
        UserChat unrelatedUserChat4 = createUserChat(unrelatedChat2, chatPartner1);

        // Delete the user
        entityManager.flush();
        entityManager.clear();
        userRepository.deleteById(userToDelete.getUserId());
        entityManager.flush();

        // Then: User's UserChats should be deleted, but chats and partner UserChats remain
        assertFalse(userRepository.existsById(userToDelete.getUserId()));
        assertFalse(userChatRepository.existsById(userChat1.getUserChatId()));
        assertFalse(userChatRepository.existsById(userChat2.getUserChatId()));
        assertFalse(userChatRepository.existsById(partnerChat1.getUserChatId()));
        assertFalse(userChatRepository.existsById(partnerChat2.getUserChatId()));
        assertFalse(chatRepository.existsById(chat1.getChatId()));
        assertFalse(chatRepository.existsById(chat2.getChatId()));

        // Unrelated entities should remain
        assertTrue(chatRepository.existsById(unrelatedChat.getChatId()));
        assertTrue(chatRepository.existsById(unrelatedChat2.getChatId()));
        assertTrue(chatRepository.existsById(unrelatedChat3.getChatId()));
        assertTrue(userChatRepository.existsById(unrelatedUserChat1.getUserChatId()));
        assertTrue(userChatRepository.existsById(unrelatedUserChat2.getUserChatId()));
        assertTrue(userChatRepository.existsById(unrelatedUserChat3.getUserChatId()));
        assertTrue(userChatRepository.existsById(unrelatedUserChat4.getUserChatId()));
    }

    @Test
    @Transactional
    void testUserDeletion_ShouldCascadeDeleteUserMessagesOnly() {
        // Given: User with messages in chats
        String timestamp = String.valueOf(System.currentTimeMillis());
        User userToDelete = createUser("deleteUser_" + timestamp);
        User chatPartner = createUser("partner_" + timestamp);
        User unrelatedUser1 = createUser("unrelated1_" + timestamp);
        User unrelatedUser2 = createUser("unrelated2_" + timestamp);

        Chat chat = createChat(userToDelete, chatPartner);
        Chat unrelatedChat = createChat(unrelatedUser1, unrelatedUser2);

        createUserChat(chat, userToDelete);
        createUserChat(chat, chatPartner);
        createUserChat(unrelatedChat, unrelatedUser1);
        createUserChat(unrelatedChat, unrelatedUser2);

        // Create messages
        Message userMessage = createMessage(chat, userToDelete, "Message from user to delete");
        Message partnerMessage = createMessage(chat, chatPartner, "Message from partner");
        Message unrelatedMessage = createMessage(unrelatedChat, unrelatedUser1, "Unrelated message");

        // Delete the user
        entityManager.flush();
        entityManager.clear();
        userRepository.deleteById(userToDelete.getUserId());
        entityManager.flush();

        // Then: Only user's messages should be deleted
        assertFalse(userRepository.existsById(userToDelete.getUserId()));
        assertFalse(messageRepository.existsById(userMessage.getMessageId()));
        assertFalse(messageRepository.existsById(partnerMessage.getMessageId()));
        assertFalse(chatRepository.existsById(chat.getChatId()));

        // Other messages should remain
        assertTrue(messageRepository.existsById(unrelatedMessage.getMessageId()));
        assertTrue(userRepository.existsById(chatPartner.getUserId()));
        assertTrue(userRepository.existsById(unrelatedUser1.getUserId()));
        assertTrue(userRepository.existsById(unrelatedUser2.getUserId()));
    }

    @Test
    @Transactional
    void testUserDeletion_ShouldCascadeDeleteUserReactionsOnly() {
        // Given: User with message reactions
        String timestamp = String.valueOf(System.currentTimeMillis());
        User userToDelete = createUser("deleteUser_" + timestamp);
        User chatPartner = createUser("partner_" + timestamp);
        User unrelatedUser = createUser("unrelated_" + timestamp);

        Chat chat = createChat(userToDelete, chatPartner);
        Chat chat2 = createChat(chatPartner, unrelatedUser);
        createUserChat(chat, userToDelete);
        createUserChat(chat, chatPartner);
        createUserChat(chat2, chatPartner);
        createUserChat(chat2, unrelatedUser);

        Message userMessage = createMessage(chat, userToDelete, "User message");
        Message partnerMessage = createMessage(chat, chatPartner, "Partner message");

        // Create reactions
        MessageReaction userReaction = createReaction(userToDelete, partnerMessage, MessageReact.LIKE);
        MessageReaction partnerReaction = createReaction(chatPartner, userMessage, MessageReact.LIKE);

        // Delete the user
        entityManager.flush();
        entityManager.clear();
        userRepository.deleteById(userToDelete.getUserId());
        entityManager.flush();

        assertFalse(userRepository.existsById(userToDelete.getUserId()));
        assertFalse(messageReactionRepository.existsById(userReaction.getMessageReactionId()));
        assertFalse(messageReactionRepository.existsById(partnerReaction.getMessageReactionId()));
        assertFalse(messageRepository.existsById(userMessage.getMessageId())); // User's message also deleted
        assertFalse(messageRepository.existsById(partnerMessage.getMessageId()));
        assertFalse(chatRepository.existsById(chat.getChatId()));

        assertTrue(userRepository.existsById(chatPartner.getUserId()));
        assertTrue(userRepository.existsById(unrelatedUser.getUserId()));
        assertTrue(chatRepository.existsById(chat2.getChatId()));
    }

    @Test
    @Transactional
    void testUserDeletion_CompleteIntegrationTest() {
        // Given: User with all possible relationships
        String timestamp = String.valueOf(System.currentTimeMillis());
        User userToDelete = createUser("deleteUser_" + timestamp);
        User friend = createUser("friend_" + timestamp);
        User followed = createUser("followed_" + timestamp);
        User follower = createUser("follower_" + timestamp);
        User blocked = createUser("blocked_" + timestamp);
        User blocker = createUser("blocker_" + timestamp);
        User chatPartner = createUser("chatPartner_" + timestamp);

        // Create all relationships
        Friendship friendship = createFriendship(userToDelete, friend);
        Follow followRelation = createFollow(userToDelete, followed);
        Follow followedByRelation = createFollow(follower, userToDelete);
        Block blockRelation = createBlock(userToDelete, blocked);
        Block blockedByRelation = createBlock(blocker, userToDelete);
        FriendRequest sentRequest = createFriendRequest(userToDelete, followed);
        FriendRequest receivedRequest = createFriendRequest(friend, userToDelete);

        // Create chat with messages and reactions
        Chat chat = createChat(userToDelete, chatPartner);
        UserChat userChat = createUserChat(chat, userToDelete);
        UserChat partnerChat = createUserChat(chat, chatPartner);

        Message userMessage = createMessage(chat, userToDelete, "Hello");
        Message partnerMessage = createMessage(chat, chatPartner, "Hi back");

        MessageReaction userReaction = createReaction(userToDelete, partnerMessage, MessageReact.LIKE);
        MessageReaction partnerReaction = createReaction(chatPartner, userMessage, MessageReact.LIKE);


        // Verify all entities exist
        assertTrue(userRepository.existsById(userToDelete.getUserId()));
        assertTrue(friendshipRepository.existsById(friendship.getId()));
        assertTrue(followRepository.existsById(followRelation.getId()));
        assertTrue(followRepository.existsById(followedByRelation.getId()));
        assertTrue(blockRepository.existsById(blockRelation.getId()));
        assertTrue(blockRepository.existsById(blockedByRelation.getId()));
        assertTrue(friendRequestRepository.existsById(sentRequest.getId()));
        assertTrue(friendRequestRepository.existsById(receivedRequest.getId()));
        assertTrue(chatRepository.existsById(chat.getChatId()));
        assertTrue(userChatRepository.existsById(userChat.getUserChatId()));
        assertTrue(messageRepository.existsById(userMessage.getMessageId()));
        assertTrue(messageReactionRepository.existsById(userReaction.getMessageReactionId()));

        // Delete the user
        entityManager.flush();
        entityManager.clear();
        userRepository.deleteById(userToDelete.getUserId());
        entityManager.flush();

        // Then: Verify complete cascade deletion
        assertFalse(userRepository.existsById(userToDelete.getUserId()));
        assertFalse(friendshipRepository.existsById(friendship.getId()));
        assertFalse(followRepository.existsById(followRelation.getId()));
        assertFalse(followRepository.existsById(followedByRelation.getId()));
        assertFalse(blockRepository.existsById(blockRelation.getId()));
        assertFalse(blockRepository.existsById(blockedByRelation.getId()));
        assertFalse(friendRequestRepository.existsById(sentRequest.getId()));
        assertFalse(friendRequestRepository.existsById(receivedRequest.getId()));
        assertFalse(userChatRepository.existsById(userChat.getUserChatId()));
        assertFalse(messageRepository.existsById(userMessage.getMessageId()));
        assertFalse(messageReactionRepository.existsById(userReaction.getMessageReactionId()));
        assertFalse(userChatRepository.existsById(partnerChat.getUserChatId()));
        assertFalse(chatRepository.existsById(chat.getChatId()));
        assertFalse(messageRepository.existsById(partnerMessage.getMessageId()));
        assertFalse(messageReactionRepository.existsById(partnerReaction.getMessageReactionId()));

        // All other users should remain
        assertTrue(userRepository.existsById(friend.getUserId()));
        assertTrue(userRepository.existsById(followed.getUserId()));
        assertTrue(userRepository.existsById(follower.getUserId()));
        assertTrue(userRepository.existsById(blocked.getUserId()));
        assertTrue(userRepository.existsById(blocker.getUserId()));
        assertTrue(userRepository.existsById(chatPartner.getUserId()));
    }

    // Helper methods for creating entities
    private User createUser(String username) {
        return userRepository.save(User.builder()
                .userId(UUID.randomUUID())
                .username(username)
                .email(username.toLowerCase() + "@test.com")
                .build());
    }

    private Friendship createFriendship(User user1, User user2) {
        // Ensure proper ordering like in your service
        if (user1.getUserId().compareTo(user2.getUserId()) > 0) {
            User temp = user1;
            user1 = user2;
            user2 = temp;
        }
        return friendshipRepository.save(Friendship.builder()
                .user1(user1)
                .user2(user2)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build());
    }

    private Follow createFollow(User follower, User followed) {
        return followRepository.save(Follow.builder()
                .follower(follower)
                .followed(followed)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build());
    }

    private Block createBlock(User blocker, User blocked) {
        return blockRepository.save(Block.builder()
                .blocker(blocker)
                .blocked(blocked)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build());
    }

    private FriendRequest createFriendRequest(User sender, User receiver) {
        return friendRequestRepository.save(FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build());
    }

    private Chat createChat(User user1, User user2) {
        return chatRepository.save(Chat.builder()
                .user1(user1)
                .user2(user2)
                .build());
    }

    private UserChat createUserChat(Chat chat, User user) {
        return userChatRepository.save(UserChat.builder()
                .chat(chat)
                .user(user)
                .pinned(false)
                .unread(0)
                .muted(false)
                .build());
    }

    private Message createMessage(Chat chat, User sender, String content) {
        return messageRepository.save(Message.builder()
                .chat(chat)
                .sender(sender)
                .content(content)
                .isFile(false)
                .status(MessageStatus.SENT)
                .isEdited(false)
                .build());
    }

    private MessageReaction createReaction(User user, Message message, MessageReact reactionType) {
        return messageReactionRepository.save(MessageReaction.builder()
                .user(user)
                .message(message)
                .reactionType(reactionType)
                .build());
    }


    // ========== STRONG COMPREHENSIVE TEST CASES ==========

    @Test
    @Transactional
    void testUserDeletion_WithMultipleFriendshipsAndOrdering() {
        // Given: User with friendships that test ordering logic
        String timestamp = String.valueOf(System.currentTimeMillis());
        User userToDelete = createUser("aaaDeleteUser_" + timestamp); // Low alphabetically
        User userB = createUser("bbbUser_" + timestamp);
        User userC = createUser("cccUser_" + timestamp);
        User userZ = createUser("zzzUser_" + timestamp); // High alphabetically

        // Create friendships with different ordering
        Friendship friendship1 = createFriendship(userToDelete, userB); // userToDelete should be user1
        Friendship friendship2 = createFriendship(userZ, userToDelete); // userToDelete should be user2
        Friendship friendship3 = createFriendship(userToDelete, userC); // userToDelete should be user1

        // Unrelated friendship to ensure it's not affected
        Friendship unrelatedFriendship = createFriendship(userB, userC);

        // Store counts before deletion
        long friendshipCountBefore = friendshipRepository.count();
        long userCountBefore = userRepository.count();

        // Delete the user
        entityManager.flush();
        entityManager.clear();
        userRepository.deleteById(userToDelete.getUserId());
        entityManager.flush();

        // Then: Verify correct deletions and data integrity
        assertFalse(userRepository.existsById(userToDelete.getUserId()));
        assertFalse(friendshipRepository.existsById(friendship1.getId()));
        assertFalse(friendshipRepository.existsById(friendship2.getId()));
        assertFalse(friendshipRepository.existsById(friendship3.getId()));

        // Unrelated friendship should remain
        assertTrue(friendshipRepository.existsById(unrelatedFriendship.getId()));

        // Verify counts
        assertEquals(friendshipCountBefore - 3, friendshipRepository.count());
        assertEquals(userCountBefore - 1, userRepository.count());

        // All other users should remain
        assertTrue(userRepository.existsById(userB.getUserId()));
        assertTrue(userRepository.existsById(userC.getUserId()));
        assertTrue(userRepository.existsById(userZ.getUserId()));
    }

    @Test
    @Transactional
    void testUserDeletion_WithCircularFollowRelationships() {
        // Given: Users with circular follow relationships
        String timestamp = String.valueOf(System.currentTimeMillis());
        User userToDelete = createUser("deleteUser_" + timestamp);
        User userA = createUser("userA_" + timestamp);
        User userB = createUser("userB_" + timestamp);
        User userC = createUser("userC_" + timestamp);

        // Create circular follows: A -> B -> C -> A, and userToDelete involved
        Follow followAB = createFollow(userA, userB);
        Follow followBC = createFollow(userB, userC);
        Follow followCA = createFollow(userC, userA);
        Follow userFollowsA = createFollow(userToDelete, userA);
        Follow aFollowsUser = createFollow(userA, userToDelete);
        Follow userFollowsB = createFollow(userToDelete, userB);

        long followCountBefore = followRepository.count();

        // Delete the user
        entityManager.flush();
        entityManager.clear();
        userRepository.deleteById(userToDelete.getUserId());
        entityManager.flush();

        // Then: Only follows involving deleted user should be removed
        assertFalse(userRepository.existsById(userToDelete.getUserId()));
        assertFalse(followRepository.existsById(userFollowsA.getId()));
        assertFalse(followRepository.existsById(aFollowsUser.getId()));
        assertFalse(followRepository.existsById(userFollowsB.getId()));

        // Circular follows should remain intact
        assertTrue(followRepository.existsById(followAB.getId()));
        assertTrue(followRepository.existsById(followBC.getId()));
        assertTrue(followRepository.existsById(followCA.getId()));

        assertEquals(followCountBefore - 3, followRepository.count());
    }

    @Test
    @Transactional
    void testUserDeletion_WithComplexChatScenarios() {
        // Given: User in multiple chats with various message patterns
        String timestamp = String.valueOf(System.currentTimeMillis());
        User userToDelete = createUser("deleteUser_" + timestamp);
        User partner1 = createUser("partner1_" + timestamp);
        User partner2 = createUser("partner2_" + timestamp);
        User partner3 = createUser("partner3_" + timestamp);

        // Create multiple chats
        Chat chat1 = createChat(userToDelete, partner1);
        Chat chat2 = createChat(userToDelete, partner2);
        Chat chat3 = createChat(partner2, partner3); // Chat without userToDelete

        // Create UserChats
        UserChat userChat1 = createUserChat(chat1, userToDelete);
        UserChat partnerChat1 = createUserChat(chat1, partner1);
        UserChat userChat2 = createUserChat(chat2, userToDelete);
        UserChat partnerChat2 = createUserChat(chat2, partner2);
        UserChat unrelatedChat3_1 = createUserChat(chat3, partner2);
        UserChat unrelatedChat3_2 = createUserChat(chat3, partner3);

        // Create messages with different patterns
        Message userMsg1 = createMessage(chat1, userToDelete, "First message");
        Message partnerMsg1 = createMessage(chat1, partner1, "Reply 1");
        Message userMsg2 = createMessage(chat1, userToDelete, "Second message");

        Message userMsg3 = createMessage(chat2, userToDelete, "Message in chat2");
        Message partnerMsg2 = createMessage(chat2, partner2, "Reply in chat2");

        Message unrelatedMsg = createMessage(chat3, partner2, "Unrelated message");

        // Create reactions on various messages
        MessageReaction userReaction1 = createReaction(userToDelete, partnerMsg1, MessageReact.LIKE);
        MessageReaction userReaction2 = createReaction(userToDelete, partnerMsg2, MessageReact.LIKE);
        MessageReaction partnerReaction1 = createReaction(partner1, userMsg1, MessageReact.LIKE);
        MessageReaction partnerReaction2 = createReaction(partner2, userMsg3, MessageReact.LIKE);
        MessageReaction unrelatedReaction = createReaction(partner3, unrelatedMsg, MessageReact.LIKE);

        long chatCountBefore = chatRepository.count();
        long userChatCountBefore = userChatRepository.count();
        long messageCountBefore = messageRepository.count();

        // Delete the user
        entityManager.flush();
        entityManager.clear();
        userRepository.deleteById(userToDelete.getUserId());
        entityManager.flush();

        // Then: Verify detailed cascade effects
        assertFalse(userRepository.existsById(userToDelete.getUserId()));

        // User's UserChats should be deleted
        assertFalse(userChatRepository.existsById(userChat1.getUserChatId()));
        assertFalse(userChatRepository.existsById(userChat2.getUserChatId()));
        assertFalse(userChatRepository.existsById(partnerChat1.getUserChatId()));
        assertFalse(userChatRepository.existsById(partnerChat2.getUserChatId()));

        // Partner UserChats should remain
        assertTrue(userChatRepository.existsById(unrelatedChat3_1.getUserChatId()));
        assertTrue(userChatRepository.existsById(unrelatedChat3_2.getUserChatId()));
        assertTrue(chatRepository.existsById(chat3.getChatId()));

        // All chats shouldn't remain (user was part of)
        assertFalse(chatRepository.existsById(chat1.getChatId()));
        assertFalse(chatRepository.existsById(chat2.getChatId()));

        // User's messages should be deleted
        assertFalse(messageRepository.existsById(userMsg1.getMessageId()));
        assertFalse(messageRepository.existsById(userMsg2.getMessageId()));
        assertFalse(messageRepository.existsById(userMsg3.getMessageId()));

        // Partner messages shouldn't remain
        assertFalse(messageRepository.existsById(partnerMsg1.getMessageId()));
        assertFalse(messageRepository.existsById(partnerMsg2.getMessageId()));
        assertTrue(messageRepository.existsById(unrelatedMsg.getMessageId()));

        // User's reactions should be deleted
        assertFalse(messageReactionRepository.existsById(userReaction1.getMessageReactionId()));
        assertFalse(messageReactionRepository.existsById(userReaction2.getMessageReactionId()));


        // Note: This depends on your cascade configuration - adjust accordingly
        assertTrue(messageReactionRepository.existsById(unrelatedReaction.getMessageReactionId()));
    }

    @Test
    @Transactional
    void testUserDeletion_WithPendingFriendRequestsAndExistingFriendships() {
        // Given: Complex scenario with friend requests and existing friendships
        String timestamp = String.valueOf(System.currentTimeMillis());
        User userToDelete = createUser("deleteUser_" + timestamp);
        User existingFriend = createUser("existingFriend_" + timestamp);
        User pendingRequester = createUser("pendingRequester_" + timestamp);
        User pendingReceiver = createUser("pendingReceiver_" + timestamp);
        User unrelatedUser1 = createUser("unrelated1_" + timestamp);
        User unrelatedUser2 = createUser("unrelated2_" + timestamp);

        // Create existing friendship
        Friendship existingFriendship = createFriendship(userToDelete, existingFriend);

        // Create pending friend requests
        FriendRequest incomingRequest = createFriendRequest(pendingRequester, userToDelete);
        FriendRequest outgoingRequest = createFriendRequest(userToDelete, pendingReceiver);

        // Create unrelated entities
        Friendship unrelatedFriendship = createFriendship(unrelatedUser1, unrelatedUser2);
        FriendRequest unrelatedRequest = createFriendRequest(pendingRequester, unrelatedUser1);

        long friendshipCountBefore = friendshipRepository.count();
        long friendRequestCountBefore = friendRequestRepository.count();

        // Delete the user
        entityManager.flush();
        entityManager.clear();
        userRepository.deleteById(userToDelete.getUserId());
        entityManager.flush();

        // Then: All relationships involving user should be cleaned up
        assertFalse(userRepository.existsById(userToDelete.getUserId()));

        // Existing friendship should be deleted
        assertFalse(friendshipRepository.existsById(existingFriendship.getId()));

        // Pending requests should be deleted
        assertFalse(friendRequestRepository.existsById(incomingRequest.getId()));
        assertFalse(friendRequestRepository.existsById(outgoingRequest.getId()));

        // Unrelated entities should remain
        assertTrue(friendshipRepository.existsById(unrelatedFriendship.getId()));
        assertTrue(friendRequestRepository.existsById(unrelatedRequest.getId()));

        // Verify counts
        assertEquals(friendshipCountBefore - 1, friendshipRepository.count());
        assertEquals(friendRequestCountBefore - 2, friendRequestRepository.count());

        // All other users should remain
        assertTrue(userRepository.existsById(existingFriend.getUserId()));
        assertTrue(userRepository.existsById(pendingRequester.getUserId()));
        assertTrue(userRepository.existsById(pendingReceiver.getUserId()));
    }

    @Test
    @Transactional
    void testUserDeletion_WithMessageThreadsAndReplyChains() {
        // Given: User with complex message threads and reply chains
        String timestamp = String.valueOf(System.currentTimeMillis());
        User userToDelete = createUser("deleteUser_" + timestamp);
        User chatPartner = createUser("chatPartner_" + timestamp);

        Chat chat = createChat(userToDelete, chatPartner);
        createUserChat(chat, userToDelete);
        createUserChat(chat, chatPartner);

        // Create message chain: original -> reply1 -> reply2
        Message originalMsg = createMessage(chat, userToDelete, "Original message");
        Message reply1 = messageRepository.save(Message.builder()
                .chat(chat)
                .sender(chatPartner)
                .content("Reply 1")
                .replyTo(originalMsg)
                .isFile(false)
                .status(MessageStatus.SENT)
                .isEdited(false)
                .build());

        Message reply2 = messageRepository.save(Message.builder()
                .chat(chat)
                .sender(userToDelete)
                .content("Reply 2")
                .replyTo(reply1)
                .isFile(false)
                .status(MessageStatus.SENT)
                .isEdited(false)
                .build());

        Message reply3 = messageRepository.save(Message.builder()
                .chat(chat)
                .sender(chatPartner)
                .content("Reply 3")
                .replyTo(reply2)
                .isFile(false)
                .status(MessageStatus.SENT)
                .isEdited(false)
                .build());

        // Create reactions on various messages in the chain
        MessageReaction userReactionOnReply1 = createReaction(userToDelete, reply1, MessageReact.LIKE);
        MessageReaction partnerReactionOnOriginal = createReaction(chatPartner, originalMsg, MessageReact.LIKE);
        MessageReaction partnerReactionOnReply2 = createReaction(chatPartner, reply2, MessageReact.LIKE);

        // Delete the user
        entityManager.flush();
        entityManager.clear();
        userRepository.deleteById(userToDelete.getUserId());
        entityManager.flush();

        // Then: Verify message chain integrity after user deletion
        assertFalse(userRepository.existsById(userToDelete.getUserId()));

        // User's messages should be deleted
        assertFalse(messageRepository.existsById(originalMsg.getMessageId()));
        assertFalse(messageRepository.existsById(reply2.getMessageId()));

        assertFalse(messageRepository.existsById(reply1.getMessageId()));
        assertFalse(messageRepository.existsById(reply3.getMessageId()));

        // User's reactions should be deleted
        assertFalse(messageReactionRepository.existsById(userReactionOnReply1.getMessageReactionId()));
        assertFalse(messageReactionRepository.existsById(partnerReactionOnOriginal.getMessageReactionId()));
        assertFalse(messageReactionRepository.existsById(partnerReactionOnReply2.getMessageReactionId()));
    }

    @Test
    @Transactional
    void testUserDeletion_DatabaseConsistencyAfterDeletion() {
        // Given: User with maximum relationships to test database consistency
        String timestamp = String.valueOf(System.currentTimeMillis());
        User userToDelete = createUser("deleteUser_" + timestamp);

        // Create multiple users for various relationships
        User[] friends = new User[5];
        User[] followers = new User[3];
        User[] blocked = new User[2];
        User[] chatPartners = new User[4];

        for (int i = 0; i < friends.length; i++) {
            friends[i] = createUser("friend" + i + "_" + timestamp);
        }
        for (int i = 0; i < followers.length; i++) {
            followers[i] = createUser("follower" + i + "_" + timestamp);
        }
        for (int i = 0; i < blocked.length; i++) {
            blocked[i] = createUser("blocked" + i + "_" + timestamp);
        }
        for (int i = 0; i < chatPartners.length; i++) {
            chatPartners[i] = createUser("chatPartner" + i + "_" + timestamp);
        }

        // Create multiple relationships
        for (User friend : friends) {
            createFriendship(userToDelete, friend);
            createFriendRequest(userToDelete, friend); // Also create some pending requests
        }

        for (User follower : followers) {
            createFollow(follower, userToDelete);
            createFollow(userToDelete, follower); // Mutual follows
        }

        for (User blockedUser : blocked) {
            createBlock(userToDelete, blockedUser);
        }

        // Create chats with messages and reactions
        for (User partner : chatPartners) {
            Chat chat = createChat(userToDelete, partner);
            createUserChat(chat, userToDelete);
            createUserChat(chat, partner);

            Message userMsg = createMessage(chat, userToDelete, "Message to " + partner.getUsername());
            Message partnerMsg = createMessage(chat, partner, "Reply from " + partner.getUsername());

            createReaction(userToDelete, partnerMsg, MessageReact.LIKE);
            createReaction(partner, userMsg, MessageReact.LIKE);
        }

        // Store counts before deletion
        long userCountBefore = userRepository.count();
        long friendshipCountBefore = friendshipRepository.count();
        long followCountBefore = followRepository.count();
        long blockCountBefore = blockRepository.count();
        long friendRequestCountBefore = friendRequestRepository.count();

        // Delete the user
        entityManager.flush();
        entityManager.clear();
        userRepository.deleteById(userToDelete.getUserId());
        entityManager.flush();

        // Then: Verify complete database consistency
        assertFalse(userRepository.existsById(userToDelete.getUserId()));

        // Verify exact counts
        assertEquals(userCountBefore - 1, userRepository.count());
        assertEquals(friendshipCountBefore - friends.length, friendshipRepository.count());
        assertEquals(followCountBefore - (followers.length * 2), followRepository.count());
        assertEquals(blockCountBefore - blocked.length, blockRepository.count());
        assertEquals(friendRequestCountBefore - friends.length, friendRequestRepository.count());
    }
}

