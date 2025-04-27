package com.example.registrationmodule.repository;

import com.example.registrationmodule.model.entity.Post;
import com.example.registrationmodule.model.enumeration.Mood;
import com.example.registrationmodule.model.enumeration.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    Optional<Post> findByPostId(UUID postId);
    List<Post> findByUser_UserId(UUID userId);

    List<Post> findByUser_UserIdAndMood(UUID userId, Mood mood);
    List<Post> findByUser_UserIdAndPinnedTrue(UUID userId);
    List<Post> findByUser_UserIdAndVisibility(UUID userId, Visibility visibility);
}
