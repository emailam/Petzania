package com.example.friends.and.chats.module.repository;


import com.example.friends.and.chats.module.model.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {
}