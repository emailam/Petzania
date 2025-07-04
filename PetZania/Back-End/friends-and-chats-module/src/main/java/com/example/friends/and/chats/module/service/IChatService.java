package com.example.friends.and.chats.module.service;


import com.example.friends.and.chats.module.model.dto.chat.ChatDTO;
import com.example.friends.and.chats.module.model.dto.chat.UpdateUserChatDTO;
import com.example.friends.and.chats.module.model.dto.chat.UserChatDTO;

import java.util.List;
import java.util.UUID;

public interface IChatService {
    ChatDTO createChatIfNotExists(UUID user1Id, UUID user2Id);

    List<ChatDTO> getChatsForUser(UUID userId);

    UserChatDTO partialUpdateUserChat(UUID chatId, UUID userId, UpdateUserChatDTO updateUserChatDTO);

    ChatDTO getChatById(UUID chatId, UUID userId);

    void deleteUserChatById(UUID userChatId, UUID userId);

    UserChatDTO getUserChatById(UUID chatId, UUID userId);
}
