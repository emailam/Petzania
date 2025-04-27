package com.example.registrationmodule.service;
import com.example.registrationmodule.model.dto.PostDTO;
import com.example.registrationmodule.model.entity.Post;
import com.example.registrationmodule.model.enumeration.Mood;
import com.example.registrationmodule.model.enumeration.Visibility;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
public interface IPostService {
    Post createPost(UUID userId, PostDTO request) throws IOException;
    void deletePost(UUID postId);
    List<Post> getAllPosts();
    Post getPostById(UUID postId);
    public Post savePost(Post post);
    List<Post> findByUserId(UUID userId);
    List<Post> findByUserIdAndMood(UUID userId, Mood mood);
    List<Post> findByUserIdAndPinnedTrue(UUID userId);
    List<Post> findByUserIdAndVisibility(UUID userId, Visibility visibility);
}
