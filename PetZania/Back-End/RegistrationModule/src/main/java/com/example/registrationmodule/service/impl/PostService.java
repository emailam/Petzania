package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.exception.rateLimiting.TooManyPostRequests;
import com.example.registrationmodule.exception.user.UserNotFound;
import com.example.registrationmodule.model.dto.PostDTO;
import com.example.registrationmodule.model.entity.Post;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.model.enumeration.Mood;
import com.example.registrationmodule.model.enumeration.Visibility;
import com.example.registrationmodule.repository.PostRepository;
import com.example.registrationmodule.repository.UserRepository;
import com.example.registrationmodule.service.IPostService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
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
    @RateLimiter(name = "createPostLimiter", fallbackMethod = "createPostFallback")
    public Post createPost(UUID userId, PostDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFound("User not found"));
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
    @RateLimiter(name = "getAllPostsLimiter", fallbackMethod = "getAllPostsFallback")
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    @Override
    @RateLimiter(name = "getPostByIdLimiter", fallbackMethod = "getPostByIdFallback")
    public Post getPostById(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found with id: " + postId));
    }

    @Override
    @RateLimiter(name = "findPostsByUserLimiter", fallbackMethod = "findByUserIdFallback")
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

    public Post createPostFallback(UUID userId, PostDTO request, RequestNotPermitted ex) {
        throw new TooManyPostRequests("Rate limit exceeded for creating posts. Please try again later.");
    }

    public List<Post> getAllPostsFallback(RequestNotPermitted ex) {
        throw new TooManyPostRequests("Rate limit exceeded for getting all posts. Please try again later.");
    }

    public Post getPostByIdFallback(UUID postId, RequestNotPermitted ex) {
        throw new TooManyPostRequests("Rate limit exceeded while fetching post by post id. Try again later.");
    }

    public List<Post> findByUserIdFallback(UUID userId, RequestNotPermitted ex) {
        throw new TooManyPostRequests("ate limit exceeded while fetching post by user id. Please try again later.");
    }
}
