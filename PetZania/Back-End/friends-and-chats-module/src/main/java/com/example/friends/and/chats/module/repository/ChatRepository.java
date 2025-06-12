package com.example.friends.and.chats.module.repository;


import com.example.friends.and.chats.module.model.entity.Chat;
import com.example.friends.and.chats.module.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID> {
    Optional<Chat> findByUser1AndUser2(User user1, User user2);

    default Optional<Chat> findByUsers(User user1, User user2) {
        Optional<Chat> chat = findByUser1AndUser2(user1, user2);
        if (!chat.isPresent()) {
            chat = findByUser1AndUser2(user2, user1);
        }
        return chat;
    }
}