package com.example.registrationmodule.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.example.registrationmodule.exception.InvalidMediaFile;
import com.example.registrationmodule.exception.PostDoesntExist;
import com.example.registrationmodule.exception.UserDoesNotExist;
import com.example.registrationmodule.model.entity.Media;
import com.example.registrationmodule.model.entity.Post;
import com.example.registrationmodule.repository.MediaRepository;
import com.example.registrationmodule.repository.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@AllArgsConstructor
@Transactional
public class MediaService {
    private final MediaRepository mediaRepository;
    private final PostRepository postRepository;
    private final AmazonS3 s3Client;
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10 MB
    public static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024; // 50 MB
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "video/mp4");
    private final String bucketName = "petzania-media";
    @Transactional
    public String uploadMedia(MultipartFile file, UUID mediaId) throws IOException {
        String key = "media/" + mediaId + "-" + file.getOriginalFilename();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());
        s3Client.putObject(bucketName, key, file.getInputStream(), metadata);
        return key;
    }
    public Post uploadAndSaveMedia(List<MultipartFile> mediaFiles, Post post) throws IOException {
        validateFiles(mediaFiles);
        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            List<Media> mediaList = new ArrayList<>();
            for (MultipartFile file : mediaFiles) {
                String fileName = file.getOriginalFilename();
                String format = "";
                if (fileName != null && fileName.contains(".")) {
                    format = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
                }
                Media media = Media.builder()
                    .format(format)
                    .type(file.getContentType())
                    .post(post)
                    .uploadedAt(LocalDateTime.now())
                    .build();
                media = saveMedia(media);
                String key = uploadMedia(file,media.getMediaId());
                media.setUrl("https://" + bucketName + ".s3.amazonaws.com/" + key);
                media = saveMedia(media);
                mediaList.add(media);

            }
            post.setMediaList(mediaList);
        }
        return post;
    }
    private void validateFiles(List<MultipartFile> mediaFiles) {
        for (MultipartFile file : mediaFiles) {
            if (file.isEmpty()) {
                throw new InvalidMediaFile("Empty file not allowed.");
            }
            if (!ALLOWED_TYPES.contains(file.getContentType())) {
                throw new InvalidMediaFile("File: " + file.getOriginalFilename() + " has invalid file type: " + file.getContentType());
            }
            if (file.getContentType().startsWith("image/") && file.getSize() > MAX_IMAGE_SIZE){
                throw new InvalidMediaFile("Image: " + file.getOriginalFilename() + " is too large. Max allowed is 10MB.");

            } else if (file.getContentType().startsWith("video/") && file.getSize() > MAX_VIDEO_SIZE) {
                throw new InvalidMediaFile("Video: " + file.getOriginalFilename() + " is too large. Max allowed is 50MB.");
            }
        }
    }

    public void deleteAllMediaForPost(UUID postId){
        Post post = postRepository.findByPostId(postId)
                .orElseThrow(() -> new PostDoesntExist("Post not found"));
        List<Media> mediaList = post.getMediaList();
        for(Media media : mediaList){
            deleteById(media.getMediaId());
        }
    }

    public Media saveMedia(Media media) {
        return mediaRepository.save(media);
    }
    public boolean existsById(UUID mediaId) {
        return mediaRepository.existsById(mediaId);
    }
    public Optional<Media> getMediaByID(UUID mediaId) { return mediaRepository.findById(mediaId); }
    public Optional<Media> getMediaByUrl(String url) { return mediaRepository.findByUrl(url); }
    public void deleteById(UUID mediaId) {
        if (!mediaRepository.existsById(mediaId)) {
            throw new EntityNotFoundException("Media not found with ID: " + mediaId);
        }
        mediaRepository.deleteById(mediaId);
    }
}
