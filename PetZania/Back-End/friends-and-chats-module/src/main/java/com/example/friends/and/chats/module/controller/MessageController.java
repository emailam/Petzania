package com.example.friends.and.chats.module.controller;

import com.example.friends.and.chats.module.model.dto.MessageDTO;
import com.example.friends.and.chats.module.model.dto.MessageEventDTO;
import com.example.friends.and.chats.module.model.dto.SendMessageDTO;
import com.example.friends.and.chats.module.model.enumeration.EventType;
import com.example.friends.and.chats.module.service.IMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final IMessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/send")
    public ResponseEntity<MessageDTO> sendMessage(@RequestBody SendMessageDTO sendMessageDTO) {
        //get user from authentication context
        UUID userId = UUID.randomUUID();
        MessageDTO saved = messageService.sendMessage(sendMessageDTO, userId);
        messagingTemplate.convertAndSend(
                "/topic/chats/" + sendMessageDTO.getChatId(),
                new MessageEventDTO(saved, EventType.SEND)
        );
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/chat/{chatId}")
    public ResponseEntity<Page<MessageDTO>> getMessagesForChat(
            @PathVariable UUID chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        //get user from authentication context
        UUID userId = UUID.randomUUID();
        Page<MessageDTO> historyPage = messageService.getMessagesForChat(chatId, userId, page, size);
        return ResponseEntity.ok(historyPage);
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<MessageDTO> getMessageById(@PathVariable UUID messageId) {
        //get user from authentication context
        UUID userId = UUID.randomUUID();
        return ResponseEntity.ok(messageService.getMessageById(messageId, userId));
    }

    // edit, delete, react, update message status

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable UUID messageId) {
        //get user from authentication context
        UUID userId = UUID.randomUUID();
        MessageDTO deletedMessage = messageService.deleteMessage(messageId, userId);

        messagingTemplate.convertAndSend(
                "/topic/chats/" + deletedMessage.getChatId(),
                new MessageEventDTO(deletedMessage, EventType.DELETE)
        );

        return ResponseEntity.noContent().build();
    }



}
