package com.example.friends.and.chats.module.repository;

import com.example.friends.and.chats.module.model.entity.Follow;
import com.example.friends.and.chats.module.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {
    boolean existsByFollowerAndFollowed(User follower, User followed);

    Optional<Follow> findByFollowerAndFollowed(User follower, User followed);

    List<Follow> findByFollower(User follower); // Who I follow

    List<Follow> findByFollowed(User followed); // My followers

    void deleteByFollowerAndFollowed(User follower, User followed);
}
