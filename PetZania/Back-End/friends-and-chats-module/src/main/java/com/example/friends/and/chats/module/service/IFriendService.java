package com.example.friends.and.chats.module.service;

import com.example.friends.and.chats.module.model.dto.FriendRequestDTO;
import com.example.friends.and.chats.module.model.dto.FriendshipDTO;

import java.util.UUID;

public interface IFriendService {
    public FriendRequestDTO sendFriendRequest(UUID senderId, UUID receiveId);

    public FriendshipDTO acceptFriendRequest(UUID requestId);

    public void declineFriendRequest(UUID requestId);
    public void removeFriend(UUID userId, UUID friendId);

}
