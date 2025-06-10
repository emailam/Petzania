package com.example.friends.and.chats.module.repository;


import com.example.friends.and.chats.module.model.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {
    Optional<MessageReaction> findByMessage_MessageIdAndUser_UserId(UUID messageId, UUID userId);
    List<MessageReaction> findByMessage_MessageId(UUID messageId);
}