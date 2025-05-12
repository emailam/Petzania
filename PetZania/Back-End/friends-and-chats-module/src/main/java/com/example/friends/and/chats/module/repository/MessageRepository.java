package com.example.friends.and.chats.module.repository;


import com.example.friends.and.chats.module.model.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    Page<Message> findByChat_ChatId(UUID chatId, Pageable pageable);
}