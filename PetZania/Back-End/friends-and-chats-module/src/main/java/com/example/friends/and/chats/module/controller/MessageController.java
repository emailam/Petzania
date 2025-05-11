package com.example.friends.and.chats.module.controller;

import com.example.friends.and.chats.module.service.IMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

//    private final IMessageService messageService;
//    private final SimpMessagingTemplate messagingTemplate;

//    @PostMapping("/send")
//    public ResponseEntity<MessageDTO> sendMessage(@RequestBody SendMessageDTO dto) {
//    get user from authentication context
//        // 1) Persist
//        MessageDTO saved = messageService.sendMessage(dto);
//
//        // 2) Broadcast to all subscribers of this chat topic
//        messagingTemplate.convertAndSend(
//                "/topic/chats/" + dto.getChatId(),
//                saved
//        );
//
//        return ResponseEntity.ok(saved);
//    }


//    @DeleteMapping("/{messageId}")
//    public ResponseEntity<Void> deleteMessage(@PathVariable UUID messageId) {
//        // 1) Delete from DB
//        messageService.deleteMessage(messageId);
//
//        // 2) Broadcast deletion event (could use a DTO or just the ID)
//        messagingTemplate.convertAndSend(
//                "/topic/chats/deleted/" + messageId,
//                messageId
//        );
//
//        return ResponseEntity.noContent().build();
//    }


//    @GetMapping("/chat/{chatId}")
//    public ResponseEntity<List<MessageDTO>> getMessagesForChat(@PathVariable UUID chatId) {
//        List<MessageDTO> history = messageService.getMessagesForChat(chatId);
//        return ResponseEntity.ok(history);
//    }
}
