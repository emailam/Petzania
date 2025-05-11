package com.example.friends.and.chats.module.service;


import com.example.friends.and.chats.module.model.dto.ChatDTO;
import com.example.friends.and.chats.module.model.dto.UserChatDTO;
import com.example.friends.and.chats.module.model.entity.Chat;
import com.example.friends.and.chats.module.model.entity.UserChat;

public interface IDTOConversionService {
    ChatDTO mapToChatDTO(Chat chat);

    Chat mapToChat(ChatDTO chatDTO);

    UserChatDTO mapToUserChatDTO(UserChat userChat);
}
