package com.example.friends.and.chats.module.repository;


import com.example.friends.and.chats.module.model.entity.UserChat;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserChatRepository extends JpaRepository<UserChat, UUID> {
    Optional<UserChat> findByChat_ChatIdAndUser_UserId(UUID chatId, UUID userId);

    List<UserChat> findByUser_UserId(UUID userId);

    List<UserChat> findByUser_UserId(UUID userId, Sort sort);

    boolean existsByChat_ChatIdAndUser_UserId(UUID chatId, UUID userId);

    @Query("SELECT COALESCE(SUM(uc.unread), 0) FROM UserChat uc WHERE uc.user.userId = :userId")
    long getTotalUnreadCount(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserChat uc SET uc.unread = uc.unread + 1 WHERE uc.chat.chatId = :chatId AND uc.user.userId = :userId")
    int incrementUnreadCount(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserChat uc SET uc.unread = uc.unread - 1 WHERE uc.chat.chatId = :chatId AND uc.user.userId = :userId AND uc.unread > 0")
    int decrementUnreadCount(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

}