package com.example.friends.and.chats.module.service;


import com.example.friends.and.chats.module.model.dto.friend.BlockDTO;
import com.example.friends.and.chats.module.model.dto.friend.FollowDTO;
import com.example.friends.and.chats.module.model.dto.friend.FriendDTO;
import com.example.friends.and.chats.module.model.dto.friend.FriendRequestDTO;
import com.example.friends.and.chats.module.model.dto.user.UserDTO;
import com.example.friends.and.chats.module.model.entity.*;
import com.example.friends.and.chats.module.model.dto.chat.ChatDTO;
import com.example.friends.and.chats.module.model.dto.message.MessageDTO;
import com.example.friends.and.chats.module.model.dto.chat.UserChatDTO;
import com.example.friends.and.chats.module.model.dto.message.MessageReactionDTO;
import com.example.friends.and.chats.module.model.entity.Chat;
import com.example.friends.and.chats.module.model.entity.Message;
import com.example.friends.and.chats.module.model.entity.MessageReaction;
import com.example.friends.and.chats.module.model.entity.UserChat;

public interface IDTOConversionService {
    ChatDTO mapToChatDTO(Chat chat);

    Chat mapToChat(ChatDTO chatDTO);

    FollowDTO mapToFollowDTO(Follow follow);

    Follow mapToFollow(FollowDTO followDTO);

    BlockDTO mapToBlockDTO(Block block);

    Block mapToBlock(BlockDTO blockDTO);

    FriendRequestDTO mapToFriendRequestDTO(FriendRequest friendRequest);

    FriendRequest mapToFriendRequest(FriendRequestDTO friendRequestDTO);

    FriendDTO mapToFriendDTO(Friendship friendship, User user);

    UserDTO mapToUserDTO(User user);
  
    UserChatDTO mapToUserChatDTO(UserChat userChat);

    MessageDTO mapToMessageDTO(Message message);

    MessageReactionDTO mapToMessageReactionDTO(MessageReaction messageReaction);

}
