package com.example.friends.and.chats.module.service;


import com.example.friends.and.chats.module.model.dto.ChatDTO;

import java.util.UUID;

public interface IChatService {
    ChatDTO createChatIfNotExists(UUID user1Id, UUID user2Id);
}
