package com.example.friends.and.chats.module.service.impl;

import ch.qos.logback.classic.spi.IThrowableProxy;
import com.example.friends.and.chats.module.exception.chat.ChatNotFound;
import com.example.friends.and.chats.module.exception.chat.UserChatNotFound;
import com.example.friends.and.chats.module.exception.message.InvalidMessageStatusTransition;
import com.example.friends.and.chats.module.exception.message.MessageNotFound;
import com.example.friends.and.chats.module.exception.message.MessageNotUpdatable;
import com.example.friends.and.chats.module.exception.user.ForbiddenOperation;
import com.example.friends.and.chats.module.exception.user.UserAccessDenied;
import com.example.friends.and.chats.module.exception.user.UserNotFound;
import com.example.friends.and.chats.module.model.dto.message.*;
import com.example.friends.and.chats.module.model.entity.*;
import com.example.friends.and.chats.module.model.enumeration.EventType;
import com.example.friends.and.chats.module.model.enumeration.MessageReact;
import com.example.friends.and.chats.module.model.enumeration.MessageStatus;
import com.example.friends.and.chats.module.repository.*;
import com.example.friends.and.chats.module.service.IDTOConversionService;
import com.example.friends.and.chats.module.service.IMessageService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@AllArgsConstructor
@Transactional
@Slf4j
public class MessageService implements IMessageService {
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final UserChatRepository userChatRepository;
    private final MessageRepository messageRepository;
    private final BlockRepository blockRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final IDTOConversionService dtoConversionService;
    private final SimpMessagingTemplate messagingTemplate;

    private void notifyUserWithUnreadCount(UUID userId, UnreadCountUpdateDTO unreadCountUpdateDTO) {
        log.info("Notifying user: {} with unread count update dto: {}", userId, unreadCountUpdateDTO);
        messagingTemplate.convertAndSend(
                "/topic/" + userId.toString() + "/unread-count",
                unreadCountUpdateDTO
        );
    }

    @Override
    public MessageDTO sendMessage(SendMessageDTO sendMessageDTO, UUID senderId) {
        Chat chat = chatRepository.findById(sendMessageDTO.getChatId())
                .orElseThrow(() -> new ChatNotFound("Chat not found"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFound("Sender not found"));

        User receiver;
        if (chat.getUser1().getUserId().equals(senderId)) {
            receiver = chat.getUser2();
        } else {
            receiver = chat.getUser1();
        }

        if (blockRepository.existsByBlockerAndBlocked(sender, receiver) || blockRepository.existsByBlockerAndBlocked(receiver, sender)) {
            throw new ForbiddenOperation("Cannot Perform this Operation due to existing block relationship");
        }

        Message replyTo = null;
        if (sendMessageDTO.getReplyToMessageId() != null) {
            replyTo = messageRepository.findById(sendMessageDTO.getReplyToMessageId())
                    .orElseThrow(() -> new MessageNotFound("Replied-to message not found"));
        }

        Optional<UserChat> userChat = userChatRepository.findByChat_ChatIdAndUser_UserId(chat.getChatId(), senderId);
        if (userChat.isEmpty()) {
            throw new UserAccessDenied("You can only send messages in your own chats");
        }

        chat.setLastMessageTimestamp(LocalDateTime.now());
        chat = chatRepository.save(chat);

        Message message = Message.builder()
                .chat(chat)
                .sender(sender)
                .content(sendMessageDTO.getContent())
                .replyTo(replyTo)
                .isFile(sendMessageDTO.isFile())
                .status(MessageStatus.SENT)
                .isEdited(false)
                .build();

        log.info("Saving the message: {}", message);
        Message saved = messageRepository.saveAndFlush(message);
        MessageDTO savedDTO = dtoConversionService.mapToMessageDTO(saved);

        userChatRepository.incrementUnreadCount(chat.getChatId(), receiver.getUserId());
        long totalUnread = userChatRepository.getTotalUnreadCount(receiver.getUserId());
        log.info("Updated unread count: {} for user: {} and with id: {}", totalUnread, receiver.getUsername(), receiver.getUserId());

        messagingTemplate.convertAndSend(
                "/topic/" + receiver.getUserId().toString() + "/messages",
                new MessageEventDTO(savedDTO, EventType.SEND)
        );

        UserChat receiverUserChat = userChatRepository.findByChat_ChatIdAndUser_UserId(chat.getChatId(), receiver.getUserId()).orElseThrow(() -> new UserChatNotFound("User chat not found"));
        UnreadCountUpdateDTO unreadCountUpdateDTO = new UnreadCountUpdateDTO();
        unreadCountUpdateDTO.setTotalUnreadCount(totalUnread);
        unreadCountUpdateDTO.setUserChatUnreadCount(receiverUserChat.getUnread());
        unreadCountUpdateDTO.setUserChatId(receiverUserChat.getUserChatId());
        notifyUserWithUnreadCount(receiver.getUserId(), unreadCountUpdateDTO);
        return savedDTO;
    }

    @Override
    public Page<MessageDTO> getMessagesByChat(UUID chatId, UUID userId, int page, int size) {
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

        Optional<UserChat> userChat = userChatRepository.findByChat_ChatIdAndUser_UserId(message.getChat().getChatId(), userId);
        if (userChat.isEmpty()) {
            throw new UserAccessDenied("You can only get messages in your own chats");
        }

        return dtoConversionService.mapToMessageDTO(message);
    }

    @Override
    public void deleteMessage(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFound("Message not found"));


        Optional<UserChat> userChat = userChatRepository.findByChat_ChatIdAndUser_UserId(message.getChat().getChatId(), userId);
        if (userChat.isEmpty()) {
            throw new UserAccessDenied("You can only delete messages in your own chats");
        }

        if (!message.getSender().getUserId().equals(userId)) {
            throw new UserAccessDenied("You can only delete messages that you sent");
        }

        User receiver;
        if (message.getChat().getUser1().getUserId().equals(userId)) {
            receiver = message.getChat().getUser2();
        } else {
            receiver = message.getChat().getUser1();
        }

        MessageDTO deletedMessageDTO = dtoConversionService.mapToMessageDTO(message);
        boolean wasUnread = !message.getStatus().equals(MessageStatus.READ);
        messagingTemplate.convertAndSend(
                "/topic/" + receiver.getUserId().toString() + "/messages",
                new MessageEventDTO(deletedMessageDTO, EventType.DELETE)
        );

        if (wasUnread) {
            userChatRepository.decrementUnreadCount(message.getChat().getChatId(), receiver.getUserId());
            long totalUnread = userChatRepository.getTotalUnreadCount(receiver.getUserId());

            UserChat receiverUserChat = userChatRepository.findByChat_ChatIdAndUser_UserId(message.getChat().getChatId(), receiver.getUserId()).orElseThrow(() -> new UserChatNotFound("User chat not found"));
            UnreadCountUpdateDTO unreadCountUpdateDTO = new UnreadCountUpdateDTO();
            unreadCountUpdateDTO.setTotalUnreadCount(totalUnread);
            unreadCountUpdateDTO.setUserChatUnreadCount(receiverUserChat.getUnread());
            unreadCountUpdateDTO.setUserChatId(receiverUserChat.getUserChatId());
            notifyUserWithUnreadCount(receiver.getUserId(), unreadCountUpdateDTO);
        }
        messageRepository.deleteById(messageId);
    }

    @Override
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

        User receiver;
        if (message.getChat().getUser1().getUserId().equals(userId)) {
            receiver = message.getChat().getUser2();
        } else {
            receiver = message.getChat().getUser1();
        }

        MessageDTO updatedMessageDTO = dtoConversionService.mapToMessageDTO(message);
        messagingTemplate.convertAndSend(
                "/topic/" + receiver.getUserId().toString() + "/messages",
                new MessageEventDTO(updatedMessageDTO, EventType.EDIT)
        );
        return updatedMessageDTO;
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

        User receiver;
        if (message.getChat().getUser1().getUserId().equals(userId)) {
            receiver = message.getChat().getUser2();
        } else {
            receiver = message.getChat().getUser1();
        }
        MessageDTO updatedMessageDTO = dtoConversionService.mapToMessageDTO(message);

        messagingTemplate.convertAndSend(
                "/topic/" + receiver.getUserId().toString() + "/messages",
                new MessageEventDTO(updatedMessageDTO, EventType.UPDATE_STATUS)
        );

        if (newStatus == MessageStatus.READ) {
            userChatRepository.decrementUnreadCount(message.getChat().getChatId(), userId);
            long totalUnread = userChatRepository.getTotalUnreadCount(userId);

            UserChat readerUserChat = userChatRepository.findByChat_ChatIdAndUser_UserId(message.getChat().getChatId(), userId).orElseThrow(() -> new UserChatNotFound("User chat not found"));
            UnreadCountUpdateDTO unreadCountUpdateDTO = new UnreadCountUpdateDTO();
            unreadCountUpdateDTO.setTotalUnreadCount(totalUnread);
            unreadCountUpdateDTO.setUserChatUnreadCount(readerUserChat.getUnread());
            unreadCountUpdateDTO.setUserChatId(readerUserChat.getUserChatId());
            notifyUserWithUnreadCount(userId, unreadCountUpdateDTO);
        }
        return updatedMessageDTO;
    }

    @Override
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
            message.getReactions().add(reaction);
        }

        MessageReaction messageReaction = messageReactionRepository.save(reaction);

        User receiver;
        if (message.getChat().getUser1().getUserId().equals(userId)) {
            receiver = message.getChat().getUser2();
        } else {
            receiver = message.getChat().getUser1();
        }

        MessageReactionDTO messageReactionDTO = dtoConversionService.mapToMessageReactionDTO(messageReaction);
        messagingTemplate.convertAndSend(
                "/topic/" + receiver.getUserId().toString() + "/reactions",
                new MessageReactionEventDTO(messageReactionDTO, EventType.REACT)
        );

        return messageReactionDTO;
    }

    @Override
    public void removeReaction(UUID messageId, UUID userId) {
        MessageReaction messageReaction = messageReactionRepository
                .findByMessage_MessageIdAndUser_UserId(messageId, userId)
                .orElseThrow(() -> new MessageNotFound("Reaction not found"));

        Message message = messageReaction.getMessage();
        message.getReactions().remove(messageReaction);

        messageReactionRepository.delete(messageReaction);

        User receiver;
        if (message.getChat().getUser1().getUserId().equals(userId)) {
            receiver = message.getChat().getUser2();
        } else {
            receiver = message.getChat().getUser1();
        }
        MessageReactionDTO messageReactionDTO = dtoConversionService.mapToMessageReactionDTO(messageReaction);
        messagingTemplate.convertAndSend(
                "/topic/" + receiver.getUserId().toString() + "/reactions",
                new MessageReactionEventDTO(messageReactionDTO, EventType.REMOVE_REACT)
        );
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

    @Override
    public long getTotalUnreadCount(UUID userId) {
        long result = userChatRepository.getTotalUnreadCount(userId);
        log.info("Total unread count for user: {} equals: {}", userId, result);
        return result;
    }
}
