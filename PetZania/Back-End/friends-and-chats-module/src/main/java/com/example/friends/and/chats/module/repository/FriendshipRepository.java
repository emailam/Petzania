package com.example.friends.and.chats.module.repository;

import com.example.friends.and.chats.module.model.entity.Friendship;
import com.example.friends.and.chats.module.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {
    boolean existsByUser1AndUser2(User user1, User user2);

    void deleteByUser1AndUser2(User user1, User user2);

    Optional<Friendship> findByUser1AndUser2(User user1, User user2);

    List<Friendship> findAllByUser1OrUser2(User user1, User user2);

}
