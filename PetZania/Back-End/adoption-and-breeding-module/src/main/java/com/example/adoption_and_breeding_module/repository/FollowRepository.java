package com.example.adoption_and_breeding_module.repository;

import com.example.adoption_and_breeding_module.model.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {
    List<UUID> findFollowedUserIdByFollowerUserId(UUID followerUserId);
    List<UUID> findFollowerUserIdByFollowedUserId(UUID followedUserId);
}
