package com.example.friends.and.chats.module.controller;

import com.example.friends.and.chats.module.model.dto.message.*;
import com.example.friends.and.chats.module.model.enumeration.EventType;
import com.example.friends.and.chats.module.model.enumeration.MessageReact;
import com.example.friends.and.chats.module.model.enumeration.MessageStatus;
import com.example.friends.and.chats.module.model.principal.UserPrincipal;
import com.example.friends.and.chats.module.service.IMessageService;
import com.example.friends.and.chats.module.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final IMessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/send")
    public ResponseEntity<MessageDTO> sendMessage(@RequestBody SendMessageDTO sendMessageDTO) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        MessageDTO saved = messageService.sendMessage(sendMessageDTO, userId);
        messagingTemplate.convertAndSend(
                "/topic/chats/" + sendMessageDTO.getChatId(),
                new MessageEventDTO(saved, EventType.SEND)
        );
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/chat/{chatId}")
    public ResponseEntity<Page<MessageDTO>> getMessagesByChat(
            @PathVariable UUID chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        Page<MessageDTO> historyPage = messageService.getMessagesByChat(chatId, userId, page, size);
        return ResponseEntity.ok(historyPage);
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<MessageDTO> getMessageById(@PathVariable UUID messageId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        return ResponseEntity.ok(messageService.getMessageById(messageId, userId));
    }

    @PatchMapping("/{messageId}/content")
    public ResponseEntity<MessageDTO> updateMessageContent(@PathVariable UUID messageId,
                                                           @RequestBody UpdateMessageContentDTO updateMessageContentDTO) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();

        String content = updateMessageContentDTO.getContent();

        MessageDTO updatedMessage = messageService.updateMessageContent(messageId, userId, content);
        messagingTemplate.convertAndSend(
                "/topic/chats/" + updatedMessage.getChatId(),
                new MessageEventDTO(updatedMessage, EventType.EDIT)
        );

        return ResponseEntity.ok(updatedMessage);
    }

    @PatchMapping("/{messageId}/status")
    public ResponseEntity<MessageDTO> updateMessageStatus(@PathVariable UUID messageId,
                                                          @RequestBody UpdateMessageStatusDTO updateMessageStatusDTO) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();

        MessageStatus messageStatus = updateMessageStatusDTO.getMessageStatus();

        MessageDTO updatedMessage = messageService.updateMessageStatus(messageId, userId, messageStatus);
        messagingTemplate.convertAndSend(
                "/topic/chats/" + updatedMessage.getChatId(),
                new MessageEventDTO(updatedMessage, EventType.UPDATE_STATUS)
        );

        return ResponseEntity.ok(updatedMessage);
    }

    @PutMapping("/{messageId}/reaction")
    public ResponseEntity<MessageReactionDTO> reactToMessage(@PathVariable UUID messageId,
                                                             @RequestBody UpdateMessageReactDTO updateMessageReact) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();

        MessageReact messageReact = updateMessageReact.getMessageReact();

        MessageReactionDTO messageReactionDTO = messageService.reactToMessage(messageId, userId, messageReact);
        messagingTemplate.convertAndSend(
                "/topic/chats/" + messageService.getChatIdFromMessageId(messageId),
                new MessageReactionEventDTO(messageReactionDTO, EventType.REACT)
        );

        return ResponseEntity.ok(messageReactionDTO);
    }

    // remove react
    @DeleteMapping("/{messageId}/reaction")
    public ResponseEntity<Void> removeReactionFromMessage(@PathVariable UUID messageId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();

        MessageReactionDTO messageReactionDTO = messageService.removeReaction(messageId, userId);
        messagingTemplate.convertAndSend(
                "/topic/chats/" + messageService.getChatIdFromMessageId(messageId),
                new MessageReactionEventDTO(messageReactionDTO, EventType.REMOVE_REACT)
        );


        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{messageId}/reactions")
    public ResponseEntity<List<MessageReactionDTO>> getReactionsForMessage(@PathVariable UUID messageId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        return ResponseEntity.ok(messageService.getReactionsForMessage(messageId, userId));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable UUID messageId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        MessageDTO deletedMessage = messageService.deleteMessage(messageId, userId);

        messagingTemplate.convertAndSend(
                "/topic/chats/" + deletedMessage.getChatId(),
                new MessageEventDTO(deletedMessage, EventType.DELETE)
        );

        return ResponseEntity.noContent().build();
    }


}
