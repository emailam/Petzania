package com.example.friends.and.chats.module.service;


import com.example.friends.and.chats.module.model.dto.ChatDTO;
import com.example.friends.and.chats.module.model.entity.Chat;

public interface IDTOConversionService {
    ChatDTO mapToChatDTO(Chat chat);

    Chat mapToChat(ChatDTO chatDTO);
}
