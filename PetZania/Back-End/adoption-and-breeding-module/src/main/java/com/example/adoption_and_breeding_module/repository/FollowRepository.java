package com.example.adoption_and_breeding_module.repository;

import com.example.adoption_and_breeding_module.model.entity.Follow;
import com.example.adoption_and_breeding_module.model.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

    @Query("select f.followed.userId from Follow f where f.follower.userId = :followerUserId")
    List<UUID> findFollowed_UserIdByFollower_UserId(@Param("followerUserId") UUID followerUserId);

    @Query("select f.follower.userId from Follow f where f.followed.userId = :followedUserId")
    List<UUID> findFollower_UserIdByFollowed_UserId(@Param("followedUserId") UUID followedUserId);
    
    boolean existsByFollowerAndFollowed(User follower, User followed);
    Optional<Follow> findByFollowerAndFollowed(User follower, User followed);
}
