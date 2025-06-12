package com.example.friends.and.chats.module.service;

import com.example.friends.and.chats.module.model.dto.friend.BlockDTO;
import com.example.friends.and.chats.module.model.dto.friend.FollowDTO;
import com.example.friends.and.chats.module.model.dto.friend.FriendRequestDTO;
import com.example.friends.and.chats.module.model.dto.friend.FriendshipDTO;
import com.example.friends.and.chats.module.model.entity.Friendship;
import com.example.friends.and.chats.module.model.entity.User;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface IFriendService {
    FriendRequestDTO sendFriendRequest(UUID senderId, UUID receiveId);

    FriendshipDTO acceptFriendRequest(UUID requestId, UUID receiverId);

    void declineFriendRequest(UUID requestId);

    Friendship createFriendship(User user1, User user2);

    void removeFriend(UUID userId, UUID friendId);

    void unblockUser(UUID blockerId, UUID blockedId);

    BlockDTO blockUser(UUID blockerId, UUID blockedId);

    void unfollowUser(UUID followerId, UUID followedId);

    FollowDTO followUser(UUID followerId, UUID followedId);

    boolean isFriendshipExists(User user1, User user2);

    boolean isBlockingExists(User user1, User user2);

    Page<FollowDTO> getFollowing(UUID userId, int page, int size, String sortBy, String direction);

    Page<FollowDTO> getFollowers(UUID userId, int page, int size, String sortBy, String direction);

    Page<BlockDTO> getBlockedUsers(UUID userId, int page, int size, String sortBy, String direction);

    Page<FriendshipDTO> getFriendships(UUID userId, int page, int size, String sortBy, String direction);
    Page<FriendRequestDTO> getReceivedFriendRequests(UUID userId, int page, int size, String sortBy, String direction);

    int getFollowingCount(UUID userId);

    int getFollowersCount(UUID userId);

    int getBlockedUsersCount(UUID userId);

    int getNumberOfFriends(UUID userId);
}
