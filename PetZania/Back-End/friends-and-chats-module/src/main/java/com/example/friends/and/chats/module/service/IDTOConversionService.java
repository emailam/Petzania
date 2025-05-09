package com.example.friendsAndChatsModule.service;

import com.example.friendsAndChatsModule.model.dto.ChatDTO;
import com.example.friendsAndChatsModule.model.entity.Chat;

public interface IDTOConversionService {
    ChatDTO mapToChatDTO(Chat chat);

    Chat mapToChat(ChatDTO chatDTO);
}
