package com.example.friends.and.chats.module.service;


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

    UserChatDTO mapToUserChatDTO(UserChat userChat);

    MessageDTO mapToMessageDTO(Message message);

    MessageReactionDTO mapToMessageReactionDTO(MessageReaction messageReaction);
}
