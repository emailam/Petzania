package com.example.friends.and.chats.module.controller;


import com.example.friends.and.chats.module.model.dto.chat.ChatDTO;
import com.example.friends.and.chats.module.model.dto.chat.UpdateUserChatDTO;
import com.example.friends.and.chats.module.model.dto.chat.UserChatDTO;
import com.example.friends.and.chats.module.model.principal.UserPrincipal;
import com.example.friends.and.chats.module.service.IChatService;
import com.example.friends.and.chats.module.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Endpoints for managing user chats and conversations")
public class ChatController {

    private final IChatService chatService;

    @Operation(summary = "Create a new chat or return existing one between two users")
    @PostMapping("/user/{user2Id}")
    public ResponseEntity<ChatDTO> createChatIfNotExists(@PathVariable(name = "user2Id") UUID user2Id) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID user1Id = userPrincipal.getUserId();
        ChatDTO chatDTO = chatService.createChatIfNotExists(user1Id, user2Id);
        return ResponseEntity.status(HttpStatus.CREATED).body(chatDTO);
    }

    @Operation(summary = "Get a specific chat by ID for the current user")
    @GetMapping("{chatId}")
    public ResponseEntity<ChatDTO> getChatById(@PathVariable(name = "chatId") UUID chatId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        return ResponseEntity.ok(chatService.getChatById(chatId, userId));
    }

    @Operation(summary = "Get all chats for the current user")
    @GetMapping
    public ResponseEntity<List<ChatDTO>> getUserChats() {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        return ResponseEntity.ok(chatService.getChatsForUser(userId));
    }

    @Operation(summary = "Partially update a user chat (like renaming, muting, etc.)")
    @PatchMapping("/{chatId}")
    public ResponseEntity<UserChatDTO> partialUpdateUserChat(@PathVariable(name = "chatId") UUID chatId,
                                                      @RequestBody UpdateUserChatDTO updateUserChatDTO) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        return ResponseEntity.ok(chatService.partialUpdateUserChat(chatId, userId, updateUserChatDTO));
    }

    @Operation(summary = "Delete a user chat entry (without affecting the other user)")
    @DeleteMapping("/user/{userChatId}")
    public ResponseEntity<Void> deleteUserChatById(@PathVariable(name = "userChatId") UUID userChatId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        chatService.deleteUserChatById(userChatId, userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

