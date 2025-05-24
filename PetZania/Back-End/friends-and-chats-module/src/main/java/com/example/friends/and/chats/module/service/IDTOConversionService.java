package com.example.friends.and.chats.module.service;


import com.example.friends.and.chats.module.model.dto.*;
import com.example.friends.and.chats.module.model.entity.*;

public interface IDTOConversionService {
    ChatDTO mapToChatDTO(Chat chat);

    Chat mapToChat(ChatDTO chatDTO);

    FollowDTO mapToFollowDTO(Follow follow);

    Follow mapToFollow(FollowDTO followDTO);

    BlockDTO mapToBlockDTO(Block block);

    Block mapToBlock(BlockDTO blockDTO);

    FriendRequestDTO mapToFriendRequestDTO(FriendRequest friendRequest);

    FriendRequest mapToFriendRequest(FriendRequestDTO friendRequestDTO);

    FriendshipDTO mapToFriendshipDTO(Friendship friendship);

    Friendship mapToFriendship(FriendshipDTO friendshipDTO);

    UserDTO mapToUserDTO(User user);
}
