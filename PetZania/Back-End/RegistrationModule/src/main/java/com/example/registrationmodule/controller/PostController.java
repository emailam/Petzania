package com.example.registrationmodule.controller;

import com.example.registrationmodule.exception.user.UserNotFound;
import com.example.registrationmodule.model.dto.PostDTO;
import com.example.registrationmodule.model.dto.PostResponseDTO;
import com.example.registrationmodule.model.entity.Post;
import com.example.registrationmodule.model.enumeration.Mood;
import com.example.registrationmodule.model.enumeration.Visibility;
import com.example.registrationmodule.service.ICloudService;
import com.example.registrationmodule.service.IDTOConversionService;
import com.example.registrationmodule.service.IPostService;
import com.example.registrationmodule.service.IUserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("api/post")
@Validated
public class PostController {
    private final IPostService postService;
    private final IDTOConversionService dtoConversion;
    private final ICloudService cloudService;
    private final IUserService userService;
    @PostMapping("/create/{userId}")
    public ResponseEntity<PostResponseDTO> createPost(
            @PathVariable UUID userId,
            @Valid @RequestPart("post") PostDTO postDTO,
            @RequestPart(value = "mediaFiles", required = false) List<MultipartFile> mediaFiles) throws IOException {
        if (!userService.userExistsById(userId)) {
            throw new UserNotFound("Pet not found with ID: " + userId);
        }
        Post postNoMedia = postService.createPost(userId, postDTO);
        Post postWithMedia = cloudService.uploadAndSaveMediaWithPost(mediaFiles, postNoMedia);
        Post createdPost = postService.savePost(postWithMedia);
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoConversion.postToDto(createdPost));
    }

    @DeleteMapping("/delete/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable UUID postId) {
        cloudService.deleteAllMediaForPost(postId);
        postService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/get-all")
    public ResponseEntity<List<PostResponseDTO>> getAllPosts() {
        List<Post> posts = postService.getAllPosts();
        if (posts.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(posts.stream()
                .map(dtoConversion::postToDto)
                .toList());
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponseDTO> getPostById(@PathVariable UUID postId) {
        Post post = postService.getPostById(postId);
        return ResponseEntity.ok(dtoConversion.postToDto(post));
    }

    // Filtering Endpoints
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostResponseDTO>> getPostsByUserId(@PathVariable UUID userId) {
        List<Post> posts = postService.findByUserId(userId);
        return posts.isEmpty() ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(posts.stream().map(dtoConversion::postToDto).toList());
    }

    @GetMapping("/user/{userId}/mood/{mood}")
    public ResponseEntity<List<PostResponseDTO>> getPostsByUserIdAndMood(@PathVariable UUID userId,
                                                                         @PathVariable Mood mood) {
        List<Post> posts = postService.findByUserIdAndMood(userId, mood);
        return posts.isEmpty() ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(posts.stream().map(dtoConversion::postToDto).toList());
    }

    @GetMapping("/user/{userId}/pinned")
    public ResponseEntity<List<PostResponseDTO>> getPinnedPosts(@PathVariable UUID userId) {
        List<Post> posts = postService.findByUserIdAndPinnedTrue(userId);
        return posts.isEmpty() ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(posts.stream().map(dtoConversion::postToDto).toList());
    }

    @GetMapping("/user/{userId}/visibility/{visibility}")
    public ResponseEntity<List<PostResponseDTO>> getPostsByUserIdAndVisibility(@PathVariable UUID userId,
                                                                               @PathVariable Visibility visibility) {
        List<Post> posts = postService.findByUserIdAndVisibility(userId, visibility);
        return posts.isEmpty() ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(posts.stream().map(dtoConversion::postToDto).toList());
    }

}
