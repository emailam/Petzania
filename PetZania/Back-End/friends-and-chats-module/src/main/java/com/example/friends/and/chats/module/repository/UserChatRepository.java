package com.example.friends.and.chats.module.repository;


import com.example.friends.and.chats.module.model.entity.UserChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserChatRepository extends JpaRepository<UserChat, UUID> {
    Optional<UserChat> findByChat_ChatIdAndUser_UserId(UUID chatId, UUID userId);
    List<UserChat> findByUser_UserId(UUID userId);
}