package com.example.friends.and.chats.module.service.impl;

import com.example.friends.and.chats.module.exception.user.*;
import com.example.friends.and.chats.module.model.dto.friend.BlockDTO;
import com.example.friends.and.chats.module.model.dto.friend.FollowDTO;
import com.example.friends.and.chats.module.model.dto.friend.FriendRequestDTO;
import com.example.friends.and.chats.module.model.dto.friend.FriendshipDTO;
import com.example.friends.and.chats.module.model.entity.*;
import com.example.friends.and.chats.module.repository.*;
import com.example.friends.and.chats.module.service.IDTOConversionService;
import com.example.friends.and.chats.module.service.IFriendService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
            throw new InvalidOperation("Cannot perform this action on yourself");
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

    public void validateAlreadyFriends(UUID senderId, UUID receiverId) {
        if (senderId.compareTo(receiverId) > 0) {
            UUID temp = senderId;
            senderId = receiverId;
            receiverId = temp;
        }
        User user1 = getUser(senderId);
        User user2 = getUser(receiverId);
        if (friendshipRepository.existsByUser1AndUser2(user1, user2)) {
            throw new FriendshipAlreadyExist("Friendship Already exists between both users");
        }
    }

    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFound("User not found with ID: " + userId));
    }

    @Override
    public FriendRequestDTO sendFriendRequest(UUID senderId, UUID receiverId) {
        validateSelfOperation(senderId, receiverId);
        validateAlreadyFriends(senderId, receiverId);
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

    @Override
    public Friendship createFriendship(User user1, User user2) {
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
    public FriendshipDTO acceptFriendRequest(UUID requestId, UUID receiverId) {
        FriendRequest request = getFriendRequest(requestId);
        if (!request.getReceiver().getUserId().equals(receiverId)) {
            throw new ForbiddenOperation("User with ID: " + receiverId + " is trying to accept a request which does not belong to him");
        }
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
        if (isFriendshipExists(user, friend)) {
            friendshipRepository.deleteByUser1AndUser2(user, friend);
        } else {
            throw new FriendshipDoesNotExist("User with ID: " + userId + " is not friend with User with ID: " + friendId);
        }
    }


    private void validateExistingFollow(User follower, User followed) {
        if (followRepository.existsByFollowerAndFollowed(follower, followed)) {
            throw new FollowingAlreadyExists("Already following this user");
        }
    }

    private void cleanupRelationships(User blocker, User blocked) {
        // Remove friend requests
        friendRequestRepository.deleteBySenderAndReceiver(blocker, blocked);
        friendRequestRepository.deleteBySenderAndReceiver(blocked, blocker);

        // Remove follows
        followRepository.deleteByFollowerAndFollowed(blocker, blocked);
        followRepository.deleteByFollowerAndFollowed(blocked, blocker);


        if (blocker.getUserId().compareTo(blocked.getUserId()) > 0) {
            User temp = blocker;
            blocker = blocked;
            blocked = temp;
        }
        // Remove friendship
        friendshipRepository.deleteByUser1AndUser2(blocker, blocked);
    }

    @Override
    public boolean isBlockingExists(User user1, User user2) {
        return blockRepository.existsByBlockerAndBlocked(user1, user2) ||
                blockRepository.existsByBlockerAndBlocked(user2, user1);
    }

    @Override
    public Page<FollowDTO> getFollowing(UUID userId, int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        User follower = getUser(userId);
        return followRepository.findFollowsByFollower(follower, pageable).map(dtoConversionService::mapToFollowDTO);
    }

    @Override
    public Page<FollowDTO> getFollowers(UUID userId, int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        User followed = getUser(userId);
        return followRepository.findFollowsByFollowed(followed, pageable).map(dtoConversionService::mapToFollowDTO);
    }

    @Override
    public Page<BlockDTO> getBlockedUsers(UUID userId, int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        User blocker = getUser(userId);
        return blockRepository.findBlocksByBlocker(blocker, pageable).map(dtoConversionService::mapToBlockDTO);
    }

    @Override
    public Page<FriendshipDTO> getFriendships(UUID userId, int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return friendshipRepository.findFriendsByUserId(userId, pageable).map(dtoConversionService::mapToFriendshipDTO);
    }

    @Override
    public boolean isFriendshipExists(User user1, User user2) {
        if (user1.getUserId().compareTo(user2.getUserId()) > 0) {
            User temp = user1;
            user1 = user2;
            user2 = temp;
        }
        return friendshipRepository.existsByUser1AndUser2(user1, user2);
    }

    @Override
    public FollowDTO followUser(UUID followerId, UUID followedId) {
        validateSelfOperation(followerId, followedId);
        User follower = getUser(followerId);
        User followed = getUser(followedId);

        validateBlockRelationship(follower, followed);
        validateExistingFollow(follower, followed);

        Follow follow = Follow.builder()
                .follower(follower)
                .followed(followed)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();

        return dtoConversionService.mapToFollowDTO(followRepository.save(follow));
    }

    @Override
    public void unfollowUser(UUID followerId, UUID followedId) {
        Follow follow = followRepository.findByFollowerAndFollowed(
                getUser(followerId),
                getUser(followedId)).orElseThrow(() -> new FollowingDoesNotExist("User with ID: " + followerId + " " + "is not following the User with ID: " + followedId));
        followRepository.delete(follow);
    }

    @Override
    public BlockDTO blockUser(UUID blockerId, UUID blockedId) {
        validateSelfOperation(blockerId, blockedId);
        User blocker = getUser(blockerId);
        User blocked = getUser(blockedId);

        if (blockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            throw new BlockingAlreadyExist("Blocking Already Exist");
        }
        cleanupRelationships(blocker, blocked);
        Block block = Block.builder()
                .blocker(blocker)
                .blocked(blocked)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();

        return dtoConversionService.mapToBlockDTO(blockRepository.save(block));
    }

    @Override
    public void unblockUser(UUID blockerId, UUID blockedId) {
        Block block = blockRepository.findByBlockerAndBlocked(
                        getUser(blockerId),
                        getUser(blockedId))
                .orElseThrow(() -> new BlockingDoesNotExist("Block relationship not found"));
        blockRepository.delete(block);
    }

    @Override
    public int getFollowingCount(UUID userId) {
        User user = getUser(userId);
        return followRepository.countByFollower(user);
    }

    @Override
    public int getFollowersCount(UUID userId) {
        User user = getUser(userId);
        return followRepository.countByFollowed(user);
    }

    @Override
    public int getBlockedUsersCount(UUID userId) {
        User user = getUser(userId);
        return blockRepository.countByBlocker(user);
    }

    @Override
    public int getNumberOfFriends(UUID userId) {
        return friendshipRepository.countFriendsByUserId(userId);
    }


}
