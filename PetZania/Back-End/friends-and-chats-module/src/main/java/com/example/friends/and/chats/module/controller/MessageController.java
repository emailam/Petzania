package com.example.friends.and.chats.module.controller;

import com.example.friends.and.chats.module.model.dto.message.*;
import com.example.friends.and.chats.module.model.enumeration.EventType;
import com.example.friends.and.chats.module.model.enumeration.MessageReact;
import com.example.friends.and.chats.module.model.enumeration.MessageStatus;
import com.example.friends.and.chats.module.model.principal.UserPrincipal;
import com.example.friends.and.chats.module.service.IMessageService;
import com.example.friends.and.chats.module.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Message", description = "Endpoints for managing chat messages and reactions")
public class MessageController {

    private final IMessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @Operation(summary = "Send a new message in a chat")
    @PostMapping("/send")
    public ResponseEntity<MessageDTO> sendMessage(@RequestBody SendMessageDTO sendMessageDTO) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        MessageDTO saved = messageService.sendMessage(sendMessageDTO, userId);
        messagingTemplate.convertAndSend(
                "/topic/" + userId.toString() + "/messages",
                new MessageEventDTO(saved, EventType.SEND)
        );
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Get paginated messages for a specific chat")
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

    @Operation(summary = "Get a message by its ID")
    @GetMapping("/{messageId}")
    public ResponseEntity<MessageDTO> getMessageById(@PathVariable UUID messageId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        return ResponseEntity.ok(messageService.getMessageById(messageId, userId));
    }

    @Operation(summary = "Edit the content of a message")
    @PatchMapping("/{messageId}/content")
    public ResponseEntity<MessageDTO> updateMessageContent(@PathVariable UUID messageId,
                                                           @RequestBody UpdateMessageContentDTO updateMessageContentDTO) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();

        String content = updateMessageContentDTO.getContent();

        MessageDTO updatedMessage = messageService.updateMessageContent(messageId, userId, content);
        messagingTemplate.convertAndSend(
                "/topic/" + userId.toString() + "/messages",
                new MessageEventDTO(updatedMessage, EventType.EDIT)
        );

        return ResponseEntity.ok(updatedMessage);
    }

    @Operation(summary = "Update the status of a message (e.g., read)")
    @PatchMapping("/{messageId}/status")
    public ResponseEntity<MessageDTO> updateMessageStatus(@PathVariable UUID messageId,
                                                          @RequestBody UpdateMessageStatusDTO updateMessageStatusDTO) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();

        MessageStatus messageStatus = updateMessageStatusDTO.getMessageStatus();

        MessageDTO updatedMessage = messageService.updateMessageStatus(messageId, userId, messageStatus);
        messagingTemplate.convertAndSend(
                "/topic/" + userId.toString() + "/messages",
                new MessageEventDTO(updatedMessage, EventType.UPDATE_STATUS)
        );

        return ResponseEntity.ok(updatedMessage);
    }

    @Operation(summary = "React to a message")
    @PutMapping("/{messageId}/reaction")
    public ResponseEntity<MessageReactionDTO> reactToMessage(@PathVariable UUID messageId,
                                                             @RequestBody UpdateMessageReactDTO updateMessageReact) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();

        MessageReact messageReact = updateMessageReact.getMessageReact();

        MessageReactionDTO messageReactionDTO = messageService.reactToMessage(messageId, userId, messageReact);
        messagingTemplate.convertAndSend(
                "/topic/" + userId.toString() + "/messages",
                new MessageReactionEventDTO(messageReactionDTO, EventType.REACT)
        );

        return ResponseEntity.ok(messageReactionDTO);
    }

    // remove react
    @Operation(summary = "Remove your reaction from a message")
    @DeleteMapping("/{messageId}/reaction")
    public ResponseEntity<Void> removeReactionFromMessage(@PathVariable UUID messageId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();

        MessageReactionDTO messageReactionDTO = messageService.removeReaction(messageId, userId);
        messagingTemplate.convertAndSend(
                "/topic/" + userId.toString() + "/messages",
                new MessageReactionEventDTO(messageReactionDTO, EventType.REMOVE_REACT)
        );


        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all reactions for a message")
    @GetMapping("/{messageId}/reactions")
    public ResponseEntity<List<MessageReactionDTO>> getReactionsForMessage(@PathVariable UUID messageId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        return ResponseEntity.ok(messageService.getReactionsForMessage(messageId, userId));
    }

    @Operation(summary = "Delete a message")
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable UUID messageId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        MessageDTO deletedMessage = messageService.deleteMessage(messageId, userId);

        messagingTemplate.convertAndSend(
                "/topic/" + userId.toString() + "/messages",
                new MessageEventDTO(deletedMessage, EventType.DELETE)
        );

        return ResponseEntity.noContent().build();
    }
}
