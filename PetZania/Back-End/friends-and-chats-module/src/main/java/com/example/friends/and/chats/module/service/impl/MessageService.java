package com.example.friends.and.chats.module.service.impl;

import com.example.friends.and.chats.module.exception.RateLimitExceeded;
import com.example.friends.and.chats.module.exception.chat.ChatNotFound;
import com.example.friends.and.chats.module.exception.message.InvalidMessageStatusTransition;
import com.example.friends.and.chats.module.exception.message.MessageNotFound;
import com.example.friends.and.chats.module.exception.message.MessageNotUpdatable;
import com.example.friends.and.chats.module.exception.user.UserAccessDenied;
import com.example.friends.and.chats.module.exception.user.UserNotFound;
import com.example.friends.and.chats.module.model.dto.message.*;
import com.example.friends.and.chats.module.model.entity.*;
import com.example.friends.and.chats.module.model.enumeration.MessageReact;
import com.example.friends.and.chats.module.model.enumeration.MessageStatus;
import com.example.friends.and.chats.module.repository.*;
import com.example.friends.and.chats.module.service.IDTOConversionService;
import com.example.friends.and.chats.module.service.IMessageService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
@Transactional
public class MessageService implements IMessageService {

    private static final String MESSAGE_RATE_LIMITER = "messageServiceRateLimiter";

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final UserChatRepository userChatRepository;
    private final MessageRepository messageRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final IDTOConversionService dtoConversionService;

    @Override
    @RateLimiter(name = MESSAGE_RATE_LIMITER, fallbackMethod = "rateLimitFallbackSend")
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

    private MessageDTO rateLimitFallbackSend(SendMessageDTO sendMessageDTO, UUID senderId, RequestNotPermitted t) {
        throw new RateLimitExceeded("Too many message sends; please retry later.");
    }

    @Override
    @RateLimiter(name = MESSAGE_RATE_LIMITER, fallbackMethod = "rateLimitFallbackGetMessages")
    public Page<MessageDTO> getMessagesByChat(UUID chatId, UUID userId, int page, int size) {
        Optional<UserChat> userChat = userChatRepository.findByChat_ChatIdAndUser_UserId(chatId, userId);
        if (userChat.isEmpty()) {
            throw new UserAccessDenied("You can only get messages in your own chats");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));
        Page<Message> messagePage = messageRepository.findByChat_ChatId(chatId, pageable);

        return messagePage.map(dtoConversionService::mapToMessageDTO);
    }

    private Page<MessageDTO> rateLimitFallbackGetMessages(UUID chatId, UUID userId, int page, int size, RequestNotPermitted t) {
        throw new RateLimitExceeded("Too many requests for fetching messages; please retry later.");
    }

    @Override
    public MessageDTO getMessageById(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFound("Message not found"));

        Optional<UserChat> userChat = userChatRepository.findByChat_ChatIdAndUser_UserId(message.getChat().getChatId(), userId);
        if (userChat.isEmpty()) {
            throw new UserAccessDenied("You can only get messages in your own chats");
        }

        return dtoConversionService.mapToMessageDTO(message);
    }

    @Override
    @RateLimiter(name = MESSAGE_RATE_LIMITER, fallbackMethod = "rateLimitFallbackDelete")
    public MessageDTO deleteMessage(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFound("Message not found"));

        Optional<UserChat> userChat = userChatRepository.findByChat_ChatIdAndUser_UserId(message.getChat().getChatId(), userId);
        if (userChat.isEmpty()) {
            throw new UserAccessDenied("You can only delete messages in your own chats");
        }

        if (!message.getSender().getUserId().equals(userId)) {
            throw new UserAccessDenied("You can only delete messages that you sent");
        }

        messageRepository.deleteById(messageId);
        return dtoConversionService.mapToMessageDTO(message);
    }

    private MessageDTO rateLimitFallbackDelete(UUID messageId, UUID userId, RequestNotPermitted t) {
        throw new RateLimitExceeded("Too many delete requests; please retry later.");
    }

    @Override
    @RateLimiter(name = MESSAGE_RATE_LIMITER, fallbackMethod = "rateLimitFallbackUpdate")
    public MessageDTO updateMessageContent(UUID messageId, UUID userId, String content) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFound("Message not found"));

        Optional<UserChat> userChat = userChatRepository.findByChat_ChatIdAndUser_UserId(message.getChat().getChatId(), userId);
        if (userChat.isEmpty()) {
            throw new UserAccessDenied("You can only update messages in your own chats");
        }

        if (!message.getSender().getUserId().equals(userId)) {
            throw new UserAccessDenied("You can only update messages that you sent");
        }

        if (message.isFile()) {
            throw new MessageNotUpdatable("The message is a file and can't be updated");
        }

        message.setContent(content);
        message.setEdited(true);
        message = messageRepository.save(message);
        return dtoConversionService.mapToMessageDTO(message);
    }

    private MessageDTO rateLimitFallbackUpdate(UUID messageId, UUID userId, String content, RequestNotPermitted t) {
        throw new RateLimitExceeded("Too many message update requests; please retry later.");
    }

    @Override
    public MessageDTO updateMessageStatus(UUID messageId, UUID userId, MessageStatus newStatus) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFound("Message not found"));

        Optional<UserChat> userChat = userChatRepository.findByChat_ChatIdAndUser_UserId(
                message.getChat().getChatId(), userId);
        if (userChat.isEmpty()) {
            throw new UserAccessDenied("You can only update messages in your own chats");
        }

        if (message.getSender().getUserId().equals(userId)) {
            throw new UserAccessDenied("Sender cannot update message status");
        }

        MessageStatus currentStatus = message.getStatus();

        boolean validTransition =
                (currentStatus == MessageStatus.SENT && newStatus == MessageStatus.DELIVERED) ||
                        (currentStatus == MessageStatus.DELIVERED && newStatus == MessageStatus.READ);

        if (!validTransition) {
            throw new InvalidMessageStatusTransition(String.format(
                    "Invalid status update from %s to %s. Allowed transitions are: SENT → DELIVERED, DELIVERED → READ.",
                    currentStatus, newStatus
            ));
        }

        message.setStatus(newStatus);
        message = messageRepository.save(message);
        return dtoConversionService.mapToMessageDTO(message);
    }

    @Override
    @RateLimiter(name = MESSAGE_RATE_LIMITER, fallbackMethod = "rateLimitFallbackReact")
    public MessageReactionDTO reactToMessage(UUID messageId, UUID userId, MessageReact reactionType) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFound("Message not found"));

        Optional<UserChat> userChat = userChatRepository.findByChat_ChatIdAndUser_UserId(
                message.getChat().getChatId(), userId);
        if (userChat.isEmpty()) {
            throw new UserAccessDenied("You can only react to messages in your own chats");
        }

        Optional<MessageReaction> existingReaction = messageReactionRepository
                .findByMessage_MessageIdAndUser_UserId(messageId, userId);

        MessageReaction reaction;
        if (existingReaction.isPresent()) {
            reaction = existingReaction.get();
            reaction.setReactionType(reactionType);
        } else {
            reaction = MessageReaction.builder()
                    .message(message)
                    .user(userRepository.findById(userId)
                            .orElseThrow(() -> new UserNotFound("User not found")))
                    .reactionType(reactionType)
                    .build();
        }

        MessageReaction messageReaction = messageReactionRepository.save(reaction);

        return dtoConversionService.mapToMessageReactionDTO(messageReaction);
    }

    private MessageReactionDTO rateLimitFallbackReact(UUID messageId, UUID userId, MessageReact reactionType, RequestNotPermitted t) {
        throw new RateLimitExceeded("Too many reaction requests; please retry later.");
    }

    @Override
    public MessageReactionDTO removeReaction(UUID messageId, UUID userId) {
        MessageReaction messageReaction = messageReactionRepository
                .findByMessage_MessageIdAndUser_UserId(messageId, userId)
                .orElseThrow(() -> new MessageNotFound("Reaction not found"));

        messageReactionRepository.delete(messageReaction);
        return dtoConversionService.mapToMessageReactionDTO(messageReaction);
    }

    @Override
    public List<MessageReactionDTO> getReactionsForMessage(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFound("Message not found"));

        Optional<UserChat> userChat = userChatRepository.findByChat_ChatIdAndUser_UserId(
                message.getChat().getChatId(), userId);
        if (userChat.isEmpty()) {
            throw new UserAccessDenied("You can only view reactions in your own chats");
        }

        List<MessageReaction> reactions = messageReactionRepository.findByMessage_MessageId(messageId);
        return reactions.stream()
                .map(dtoConversionService::mapToMessageReactionDTO)
                .toList();
    }

    @Override
    public UUID getChatIdFromMessageId(UUID messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFound("Message not found"));
        return message.getChat().getChatId();
    }
}
