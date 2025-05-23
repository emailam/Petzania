package com.example.friends.and.chats.module.service.impl;

import com.example.friends.and.chats.module.exception.user.*;
import com.example.friends.and.chats.module.model.dto.FriendRequestDTO;
import com.example.friends.and.chats.module.model.dto.FriendshipDTO;
import com.example.friends.and.chats.module.model.entity.*;
import com.example.friends.and.chats.module.repository.*;
import com.example.friends.and.chats.module.service.IDTOConversionService;
import com.example.friends.and.chats.module.service.IFriendService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class FriendService implements IFriendService {
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final FollowRepository followRepository;
    private final IDTOConversionService dtoConversionService;

    public void validateSelfOperation(UUID senderId, UUID receiverId) {
        if (senderId.equals(receiverId)) {
            throw new InvalidFriendRequest("Cannot perform this action on yourself");
        }
    }

    public void validateBlockRelationship(User user1, User user2) {
        if (blockRepository.existsByBlockerAndBlocked(user1, user2) ||
                blockRepository.existsByBlockerAndBlocked(user2, user1)) {
            throw new BlockingExist("Operation blocked due to existing block relationship");
        }
    }

    private void validateExistingRequest(User sender, User receiver) {
        if (friendRequestRepository.existsBySenderAndReceiver(sender, receiver)) {
            throw new FriendRequestAlreadyExists("Friend request already exists");
        }
    }

    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFound("User not found with ID: " + userId));
    }

    @Override
    public FriendRequestDTO sendFriendRequest(UUID senderId, UUID receiverId) {
        validateSelfOperation(senderId, receiverId);
        User sender = getUser(senderId);
        User receiver = getUser(receiverId);

        validateBlockRelationship(sender, receiver);
        validateExistingRequest(sender, receiver);

        FriendRequest request = FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();

        return dtoConversionService.mapToFriendRequestDTO(friendRequestRepository.save(request));
    }

    public FriendRequest getFriendRequest(UUID requestId) {
        return friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new FriendRequestNotFound("Friend request not found"));
    }

    private Friendship createFriendship(User user1, User user2) {
        // Ensure consistent ordering to prevent duplicate entries
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

    @Override
    public FriendshipDTO acceptFriendRequest(UUID requestId) {
        FriendRequest request = getFriendRequest(requestId);

        Friendship friendship = createFriendship(request.getSender(), request.getReceiver());
        friendRequestRepository.deleteById(requestId);

        return dtoConversionService.mapToFriendshipDTO(friendship);
    }

    @Override
    public void declineFriendRequest(UUID requestId) {
        FriendRequest request = getFriendRequest(requestId);
        friendRequestRepository.delete(request);
    }

    @Override
    public void removeFriend(UUID userId, UUID friendId) {
        User user = getUser(userId);
        User friend = getUser(friendId);

        if (user.getUserId().compareTo(friend.getUserId()) > 0) {
            User temp = user;
            user = friend;
            friend = temp;
        }

        friendshipRepository.deleteByUser1AndUser2(user, friend);
    }


//    private void validateExistingFollow(User follower, User followed) {
//        if (followRepository.existsByFollowerAndFollowed(follower, followed)) {
//            throw new FollowingAlreadyExists("Already following this user");
//        }
//    }
//
//
//    private void cleanupRelationships(User blocker, User blocked) {
//        // Remove friend requests
//        friendRequestRepository.deleteBySenderAndReceiver(blocker, blocked);
//        friendRequestRepository.deleteBySenderAndReceiver(blocked, blocker);
//
//        // Remove follows
//        followRepository.deleteByFollowerAndFollowed(blocker, blocked);
//        followRepository.deleteByFollowerAndFollowed(blocked, blocker);
//
//        // Remove friendships
//        friendshipRepository.deleteByUser1AndUser2(blocker, blocked);
//        friendshipRepository.deleteByUser1AndUser2(blocked, blocker);
//    }
//
//    public boolean isBlocked(User user1, User user2) {
//        return blockRepository.existsByBlockerAndBlocked(user1, user2) ||
//                blockRepository.existsByBlockerAndBlocked(user2, user1);
//    }
//
//    public boolean isFriend(User user1, User user2) {
//        return friendshipRepository.existsByUser1AndUser2(user1, user2) ||
//                friendshipRepository.existsByUser1AndUser2(user2, user1);
//    }
//
//

//
//    // Friendship Operations
//
//    public List<FriendshipDTO> getFriends(UUID userId) {
//        User user = getUser(userId);
//        List<Friendship> friendships;
//        return friendshipRepository.findAllByUser1OrUser2(user, user).stream()
//    }
//
//    // Follow Operations
//    public FollowDTO followUser(UUID followerId, UUID followedId) {
//        validateSelfOperation(followerId, followedId);
//        User follower = getUser(followerId);
//        User followed = getUser(followedId);
//
//        validateBlockRelationship(followed, follower);
//        validateExistingFollow(follower, followed);
//
//        Follow follow = Follow.builder()
//                .follower(follower)
//                .followed(followed)
//                .createdAt(new Timestamp(System.currentTimeMillis()))
//                .build();
//
//        //return convertToFollowDTO(followRepository.save(follow));
//    }
//
//    public void unfollowUser(UUID followerId, UUID followedId) {
//        Follow follow = followRepository.findByFollowerAndFollowed(
//                getUser(followerId),
//                getUser(followedId))
//        //.orElseThrow(() -> new NotFoundException("Follow relationship not found"));
//        followRepository.delete(follow);
//    }
//
//    // Block Operations
//    public BlockDTO blockUser(UUID blockerId, UUID blockedId) {
//        validateSelfOperation(blockerId, blockedId);
//        User blocker = getUser(blockerId);
//        User blocked = getUser(blockedId);
//
//        cleanupRelationships(blocker, blocked);
//
//        Block block = Block.builder()
//                .blocker(blocker)
//                .blocked(blocked)
//                .createdAt(new Timestamp(System.currentTimeMillis()))
//                .build();
//
//        //return convertToBlockDTO(blockRepository.save(block));
//    }
//
//    public void unblockUser(UUID blockerId, UUID blockedId) {
//        Block block = blockRepository.findByBlockerAndBlocked(
//                getUser(blockerId),
//                getUser(blockedId))
//        //.orElseThrow(() -> new NotFoundException("Block relationship not found"));
//        blockRepository.delete(block);
//    }
}
