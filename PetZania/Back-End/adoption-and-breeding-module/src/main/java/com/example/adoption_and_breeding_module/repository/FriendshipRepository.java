package com.example.adoption_and_breeding_module.repository;

import com.example.adoption_and_breeding_module.model.entity.Friendship;
import com.example.adoption_and_breeding_module.model.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    @Query("select f.user2.userId from Friendship f where f.user1.userId = :user1Id")
    List<UUID> findUser2_UserIdByUser1_UserId(@Param("user1Id") UUID user1Id);

    @Query("select f.user1.userId from Friendship f where f.user2.userId = :user2Id")
    List<UUID> findUser1_UserIdByUser2_UserId(@Param("user2Id") UUID user2Id);

    boolean existsByUser1AndUser2(User user1, User user2);
}
