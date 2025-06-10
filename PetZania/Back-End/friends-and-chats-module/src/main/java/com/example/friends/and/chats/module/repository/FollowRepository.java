package com.example.friends.and.chats.module.repository;

import com.example.friends.and.chats.module.model.entity.Follow;
import com.example.friends.and.chats.module.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {
    boolean existsByFollowerAndFollowed(User follower, User followed);

    Optional<Follow> findByFollowerAndFollowed(User follower, User followed);

    void deleteByFollowerAndFollowed(User follower, User followed);

    Page<Follow> findFollowsByFollower(User follower, Pageable pageable);

    Page<Follow> findFollowsByFollowed(User followed, Pageable pageable);
    int countByFollower(User follower);
    int countByFollowed(User followed);
}
