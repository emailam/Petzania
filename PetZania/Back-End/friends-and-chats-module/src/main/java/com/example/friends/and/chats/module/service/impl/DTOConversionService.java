package com.example.friends.and.chats.module.service.impl;


import com.example.friends.and.chats.module.exception.user.UserNotFound;
import com.example.friends.and.chats.module.model.dto.chat.ChatDTO;
import com.example.friends.and.chats.module.model.dto.friend.BlockDTO;
import com.example.friends.and.chats.module.model.dto.friend.FollowDTO;
import com.example.friends.and.chats.module.model.dto.friend.FriendDTO;
import com.example.friends.and.chats.module.model.dto.friend.FriendRequestDTO;
import com.example.friends.and.chats.module.model.dto.message.MessageDTO;
import com.example.friends.and.chats.module.model.dto.chat.UserChatDTO;
import com.example.friends.and.chats.module.model.dto.message.MessageReactionDTO;
import com.example.friends.and.chats.module.model.dto.user.UserDTO;
import com.example.friends.and.chats.module.model.entity.*;
import com.example.friends.and.chats.module.repository.UserRepository;
import com.example.friends.and.chats.module.service.IDTOConversionService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class DTOConversionService implements IDTOConversionService {

    private final UserRepository userRepository;

    @Override
    public ChatDTO mapToChatDTO(Chat chat) {
        if (chat == null) {
            return null;
        }

        return ChatDTO.builder()
                .chatId(chat.getChatId())
                .user1Id(chat.getUser1().getUserId())
                .user2Id(chat.getUser2().getUserId())
                .createdAt(chat.getCreatedAt())
                .lastMessageTimestamp(chat.getLastMessageTimestamp())
                .build();
    }

    @Override
    public Chat mapToChat(ChatDTO chatDTO) {
        if (chatDTO == null) {
            return null;
        }

        User user1 = userRepository.findById(chatDTO.getUser1Id())
                .orElseThrow(() -> new UserNotFound("User1 not found"));
        User user2 = userRepository.findById(chatDTO.getUser2Id())
                .orElseThrow(() -> new UserNotFound("User2 not found"));

        return Chat.builder()
                .user1(user1)
                .user2(user2)
                .createdAt(chatDTO.getCreatedAt())
                .lastMessageTimestamp(chatDTO.getLastMessageTimestamp())
                .build();
    }

    // Friend Request conversions
    @Override
    public FriendRequestDTO mapToFriendRequestDTO(FriendRequest friendRequest) {
        if (friendRequest == null) return null;

        return FriendRequestDTO.builder()
                .requestId(friendRequest.getId())
                .sender(mapToUserDTO(friendRequest.getSender()))
                .receiver(mapToUserDTO(friendRequest.getReceiver()))
                .createdAt(friendRequest.getCreatedAt()).build();
    }
    @Override
    public UserChatDTO mapToUserChatDTO(UserChat userChat) {
        if (userChat == null)
            return null;

        return UserChatDTO.builder()
                .userChatId(userChat.getUserChatId())
                .chatId(userChat.getChat().getChatId())
                .userId(userChat.getUser().getUserId())
                .pinned(userChat.getPinned())
                .unread(userChat.getUnread())
                .muted(userChat.getMuted())

                .build();
    }

    @Override
    public FriendRequest mapToFriendRequest(FriendRequestDTO friendRequestDTO) {
        if (friendRequestDTO == null) return null;

        return FriendRequest.builder()
                .sender(getUser(friendRequestDTO.getSender().getUserId()))
                .receiver(getUser(friendRequestDTO.getReceiver().getUserId()))
                .createdAt(friendRequestDTO.getCreatedAt()).build();
    }

    public MessageDTO mapToMessageDTO(Message message) {
        if (message == null) {
            return null;
        }

        return MessageDTO.builder()
                .messageId(message.getMessageId())
                .chatId(message.getChat().getChatId())
                .senderId(message.getSender().getUserId())
                .content(message.getContent())
                .replyToMessageId(
                        Optional.ofNullable(message.getReplyTo())
                                .map(Message::getMessageId)
                                .orElse(null)
                )
                .sentAt(message.getSentAt())
                .status(message.getStatus())
                .isFile(message.isFile())
                .isEdited(message.isEdited())
                .reactions(
                        Optional.ofNullable(message.getReactions())
                                .orElseGet(Collections::emptyList)
                                .stream()
                                .map(this::mapToMessageReactionDTO)
                                .toList()
                )
                .build();
    }


    @Override
    public FriendDTO mapToFriendDTO(Friendship friendship, User user) {
        if (friendship == null) return null;

        return FriendDTO.builder()
                .friendshipId(friendship.getId())
                .friend(mapToUserDTO(user))
                .createdAt(friendship.getCreatedAt())
                .build();
    }

    @Override
    public FollowDTO mapToFollowDTO(Follow follow) {
        if (follow == null) return null;

        return FollowDTO.builder()
                .followId(follow.getId())
                .follower(mapToUserDTO(follow.getFollower()))
                .followed(mapToUserDTO(follow.getFollowed()))
                .createdAt(follow.getCreatedAt())
                .build();
    }

    @Override
    public Follow mapToFollow(FollowDTO followDTO) {
        if (followDTO == null) return null;

        return Follow.builder()
                .follower(getUser(followDTO.getFollower().getUserId()))
                .followed(getUser(followDTO.getFollowed().getUserId()))
                .createdAt(followDTO.getCreatedAt())
                .build();
    }

    @Override
    public BlockDTO mapToBlockDTO(Block block) {
        if (block == null) return null;

        return BlockDTO.builder()
                .blockId(block.getId())
                .blocker(mapToUserDTO(block.getBlocker()))
                .blocked(mapToUserDTO(block.getBlocked()))
                .createdAt(block.getCreatedAt())
                .build();
    }

    @Override
    public Block mapToBlock(BlockDTO blockDTO) {
        if (blockDTO == null) return null;

        return Block.builder()
                .blocker(getUser(blockDTO.getBlocker().getUserId()))
                .blocked(getUser(blockDTO.getBlocked().getUserId()))
                .createdAt(blockDTO.getCreatedAt())
                .build();
    }

    @Override
    public UserDTO mapToUserDTO(User user) {
        if (user == null) return null;

        return UserDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFound("User not found with ID: " + userId));
    }
  
    public MessageReactionDTO mapToMessageReactionDTO(MessageReaction messageReaction) {
        if(messageReaction == null) return null;

        return MessageReactionDTO.builder()
                .messageReactionId(messageReaction.getMessageReactionId())
                .messageId(messageReaction.getMessage().getMessageId())
                .userId(messageReaction.getUser().getUserId())
                .reactionType(messageReaction.getReactionType())
                .build();
    }
}
