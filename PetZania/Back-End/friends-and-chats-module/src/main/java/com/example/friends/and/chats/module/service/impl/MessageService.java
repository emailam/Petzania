package com.example.friends.and.chats.module.service.impl;

import com.example.friends.and.chats.module.exception.chat.ChatNotFound;
import com.example.friends.and.chats.module.exception.chat.UserChatNotFound;
import com.example.friends.and.chats.module.exception.message.MessageNotFound;
import com.example.friends.and.chats.module.exception.user.UserAccessDenied;
import com.example.friends.and.chats.module.exception.user.UserNotFound;
import com.example.friends.and.chats.module.model.dto.*;
import com.example.friends.and.chats.module.model.entity.Chat;
import com.example.friends.and.chats.module.model.entity.Message;
import com.example.friends.and.chats.module.model.entity.User;
import com.example.friends.and.chats.module.model.entity.UserChat;
import com.example.friends.and.chats.module.model.enumeration.MessageStatus;
import com.example.friends.and.chats.module.repository.*;
import com.example.friends.and.chats.module.service.IChatService;
import com.example.friends.and.chats.module.service.IDTOConversionService;
import com.example.friends.and.chats.module.service.IMessageService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
public class MessageService implements IMessageService {
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final UserChatRepository userChatRepository;
    private final MessageRepository messageRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final IDTOConversionService dtoConversionService;

    @Override
    public MessageDTO sendMessage(SendMessageDTO sendMessageDTO, UUID senderId) {
        Chat chat = chatRepository.findById(sendMessageDTO.getChatId())
                .orElseThrow(() -> new ChatNotFound("Chat not found"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFound("Sender not found"));

        Message replyTo = null;
        if (sendMessageDTO.getReplyToMessageId() != null) {
            replyTo = messageRepository.findById(sendMessageDTO.getReplyToMessageId())
                    .orElseThrow(() -> new MessageNotFound("Replied-to message not found"));
        }

        Optional<UserChat> userChat = userChatRepository.findByChat_ChatIdAndUser_UserId(chat.getChatId(), senderId);
        if (userChat.isEmpty()) {
            throw new UserAccessDenied("You can only send messages in your own chats");
        }

        Message message = Message.builder()
                .chat(chat)
                .sender(sender)
                .content(sendMessageDTO.getContent())
                .replyTo(replyTo)
                .isFile(sendMessageDTO.isFile())
                .status(MessageStatus.SENT)
                .isEdited(false)
                .build();

        Message saved = messageRepository.save(message);
        return dtoConversionService.mapToMessageDTO(saved);
    }

    @Override
    public Page<MessageDTO> getMessagesForChat(UUID chatId, UUID userId, int page, int size) {

        Optional<UserChat> userChat = userChatRepository.findByChat_ChatIdAndUser_UserId(chatId, userId);
        if (userChat.isEmpty()) {
            throw new UserAccessDenied("You can only get messages in your own chats");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));

        Page<Message> messagePage = messageRepository.findByChat_ChatId(chatId, pageable);

        return messagePage.map(dtoConversionService::mapToMessageDTO);
    }

    @Override
    public MessageDTO getMessageById(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFound("Message not found"));

        Optional<UserChat> userChat = userChatRepository.findByChat_ChatIdAndUser_UserId(message.getChat().getChatId(),
                userId);
        if (userChat.isEmpty()) {
            throw new UserAccessDenied("You can only get messages in your own chats");
        }

        return dtoConversionService.mapToMessageDTO(message);
    }

    @Override
    public MessageDTO deleteMessage(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFound("Message not found"));

        Optional<UserChat> userChat = userChatRepository.findByChat_ChatIdAndUser_UserId(message.getChat().getChatId(),
                userId);
        if (userChat.isEmpty()) {
            throw new UserAccessDenied("You can only delete messages in your own chats");
        }

        if(message.getSender().getUserId() != userId) {
            throw new UserAccessDenied("You can only delete messages that you sent");
        }

        messageRepository.deleteById(messageId);


        return dtoConversionService.mapToMessageDTO(message);
    }

}
