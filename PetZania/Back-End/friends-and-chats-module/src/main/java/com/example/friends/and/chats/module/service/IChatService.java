package com.example.friendsAndChatsModule.service;

import com.example.friendsAndChatsModule.model.dto.ChatDTO;

import java.util.UUID;

public interface IChatService {
    ChatDTO createChatIfNotExists(UUID user1Id, UUID user2Id);
}
