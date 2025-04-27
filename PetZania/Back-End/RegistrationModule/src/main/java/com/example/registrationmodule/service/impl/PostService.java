package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.exception.UserDoesNotExist;
import com.example.registrationmodule.model.dto.PostDTO;
import com.example.registrationmodule.model.entity.Post;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.model.enumeration.Mood;
import com.example.registrationmodule.model.enumeration.Visibility;
import com.example.registrationmodule.repository.PostRepository;
import com.example.registrationmodule.repository.UserRepository;
import com.example.registrationmodule.service.IPostService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
public class PostService implements IPostService {
    PostRepository postRepository;
    UserRepository userRepository;

    @Override
    public Post createPost(UUID userId, PostDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserDoesNotExist("User not found"));
        Post post = Post.builder()
                .user(user)
                .caption(request.getCaption())
                .visibility(request.getVisibility())
                .mood(request.getMood())
                .pinned(request.isPinned())
                .turnedOnNotifications(request.isTurnedOnNotifications())
                .createdAt(LocalDateTime.now())
                .build();
        return postRepository.save(post);
    }
    @Override
    public Post savePost(Post post){
        return postRepository.save(post);
    }
    @Override
    public void deletePost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found with id: " + postId));

        postRepository.delete(post);
    }
    @Override
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    @Override
    public Post getPostById(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found with id: " + postId));
    }

    @Override
    public List<Post> findByUserId(UUID userId) {
        return postRepository.findByUser_UserId(userId);
    }

    @Override
    public List<Post> findByUserIdAndMood(UUID userId, Mood mood) {
        return postRepository.findByUser_UserIdAndMood(userId, mood);
    }

    @Override
    public List<Post> findByUserIdAndPinnedTrue(UUID userId) {
        return postRepository.findByUser_UserIdAndPinnedTrue(userId);
    }

    @Override
    public List<Post> findByUserIdAndVisibility(UUID userId, Visibility visibility) {
        return postRepository.findByUser_UserIdAndVisibility(userId, visibility);
    }
}
