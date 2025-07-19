package com.example.adoption_and_breeding_module.repository;

import com.example.adoption_and_breeding_module.model.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {
    List<UUID> findUser2UserIdByUser1UserId(UUID user1Id);
    List<UUID> findUser1UserIdByUser2UserId(UUID user2Id);
}
