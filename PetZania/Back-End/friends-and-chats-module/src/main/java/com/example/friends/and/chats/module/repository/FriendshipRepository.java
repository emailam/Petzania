package com.example.friends.and.chats.module.repository;

import com.example.friends.and.chats.module.model.entity.Friendship;
import com.example.friends.and.chats.module.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {
    boolean existsByUser1AndUser2(User user1, User user2);

    void deleteByUser1AndUser2(User user1, User user2);

    @Query("SELECT f FROM Friendship f " +
            "WHERE f.user1.userId = :userId OR f.user2.userId = :userId")
    Page<Friendship> findFriendsByUserId(UUID userId, Pageable pageable);

    @Query("SELECT COUNT(f) FROM Friendship f " +
            "WHERE f.user1.userId = :userId OR f.user2.userId = :userId")
    int countFriendsByUserId(UUID userId);

    Optional<Friendship> findByUser1AndUser2(User user, User friend);
}
