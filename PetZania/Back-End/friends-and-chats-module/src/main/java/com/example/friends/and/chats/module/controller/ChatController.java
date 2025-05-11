package com.example.friends.and.chats.module.controller;


import com.example.friends.and.chats.module.model.dto.ChatDTO;
import com.example.friends.and.chats.module.model.dto.UpdateUserChatDTO;
import com.example.friends.and.chats.module.model.dto.UserChatDTO;
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

    @PostMapping("/user/{user2Id}")
    public ResponseEntity<ChatDTO> createChatIfNotExists(@PathVariable(name = "user2Id") UUID user2Id) {
        // get user1Id from authentication context
        UUID user1Id = UUID.randomUUID();
        ChatDTO chatDTO = chatService.createChatIfNotExists(user1Id, user2Id);
        return ResponseEntity.status(HttpStatus.CREATED).body(chatDTO);
    }

    @GetMapping("{chatId}")
    public ResponseEntity<ChatDTO> getChatById(@PathVariable(name = "chatId") UUID chatId) {
        // get user1Id from authentication context
        UUID userId = UUID.randomUUID();
        return ResponseEntity.ok(chatService.getChatById(chatId, userId));
    }

    @GetMapping
    public ResponseEntity<List<ChatDTO>> getUserChats() {
        // get user1Id from authentication context
        UUID userId = UUID.randomUUID();
        return ResponseEntity.ok(chatService.getChatsForUser(userId));
    }

    @PatchMapping("/{chatId}")
    public ResponseEntity<UserChatDTO> partialUpdateUserChat(@PathVariable(name = "chatId") UUID chatId,
                                                      @RequestBody UpdateUserChatDTO updateUserChatDTO) {
        // get user1Id from authentication context
        UUID userId = UUID.randomUUID();
        return ResponseEntity.ok(chatService.partialUpdateUserChat(chatId, userId, updateUserChatDTO));
    }

    @DeleteMapping("/user/{userChatId}")
    public ResponseEntity<Void> deleteUserChatById(@PathVariable(name = "userChatId") UUID userChatId) {
        // get user1Id from authentication context
        UUID userId = UUID.randomUUID();
        chatService.deleteUserChatById(userChatId, userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

