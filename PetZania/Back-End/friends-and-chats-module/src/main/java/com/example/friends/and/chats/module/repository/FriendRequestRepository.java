package com.example.friends.and.chats.module.repository;

import com.example.friends.and.chats.module.model.entity.FriendRequest;
import com.example.friends.and.chats.module.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {
    Optional<FriendRequest> findBySenderAndReceiver(User sender, User receiver);

    List<FriendRequest> findByReceiver(User receiver);

    boolean existsBySenderAndReceiver(User sender, User receiver);

    void deleteBySenderAndReceiver(User sender, User receiver);
}
