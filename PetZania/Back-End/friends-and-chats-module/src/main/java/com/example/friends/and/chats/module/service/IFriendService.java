package com.example.friends.and.chats.module.service;

import com.example.friends.and.chats.module.model.dto.BlockDTO;
import com.example.friends.and.chats.module.model.dto.FollowDTO;
import com.example.friends.and.chats.module.model.dto.FriendRequestDTO;
import com.example.friends.and.chats.module.model.dto.FriendshipDTO;
import com.example.friends.and.chats.module.model.entity.User;

import java.util.UUID;

public interface IFriendService {
    FriendRequestDTO sendFriendRequest(UUID senderId, UUID receiveId);

    FriendshipDTO acceptFriendRequest(UUID requestId);

    void declineFriendRequest(UUID requestId);

    void removeFriend(UUID userId, UUID friendId);

    void unblockUser(UUID blockerId, UUID blockedId);

    BlockDTO blockUser(UUID blockerId, UUID blockedId);

    void unfollowUser(UUID followerId, UUID followedId);

    FollowDTO followUser(UUID followerId, UUID followedId);

    boolean isFriendshipExists(User user1, User user2);

    boolean isBlockingExists(User user1, User user2);
}
