package com.example.friends.and.chats.module.controller;


import com.example.friends.and.chats.module.model.dto.ChatDTO;
import com.example.friends.and.chats.module.service.IChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final IChatService chatService;

    @PostMapping("/{user1Id}/{user2Id}")
    public ResponseEntity<ChatDTO> createChat(@PathVariable(name = "user1Id") UUID user1Id, @PathVariable(name = "user2Id") UUID user2Id) {
        ChatDTO chatDTO = chatService.createChatIfNotExists(user1Id, user2Id);
        return ResponseEntity.status(HttpStatus.CREATED).body(chatDTO);
    }

//    @GetMapping("/user/{userId}")
//    public ResponseEntity<List<ChatDTO>> getUserChats(@PathVariable UUID userId) {
//        return ResponseEntity.ok(chatService.getChatsForUser(userId));
//    }
//
//    @PatchMapping("/{chatId}/pin")
//    public ResponseEntity<Void> updatePinStatus(@PathVariable UUID chatId, @RequestParam UUID userId, @RequestParam boolean pinned) {
//        chatService.setPinned(chatId, userId, pinned);
//        return ResponseEntity.noContent().build();
//    }
//
//    @PatchMapping("/{chatId}/mute")
//    public ResponseEntity<Void> updateMuteStatus(@PathVariable UUID chatId, @RequestParam UUID userId, @RequestParam boolean muted) {
//        chatService.setMuted(chatId, userId, muted);
//        return ResponseEntity.noContent().build();
//    }
//
//    @PatchMapping("/{chatId}/unread")
//    public ResponseEntity<Void> updateUnreadStatus(@PathVariable UUID chatId, @RequestParam UUID userId, @RequestParam boolean unread) {
//        chatService.setUnread(chatId, userId, unread);
//        return ResponseEntity.noContent().build();
//    }
}

