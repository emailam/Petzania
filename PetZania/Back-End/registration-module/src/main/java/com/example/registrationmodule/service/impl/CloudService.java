package com.example.registrationmodule.service.impl;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.example.registrationmodule.config.CloudStorageConfig;
import com.example.registrationmodule.exception.media.InvalidMediaFile;
import com.example.registrationmodule.exception.media.MediaNotFound;
import com.example.registrationmodule.model.entity.Media;
import com.example.registrationmodule.repository.MediaRepository;
import com.example.registrationmodule.service.ICloudService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@AllArgsConstructor
@Profile("!dev")
@Transactional
public class CloudService implements ICloudService {
    private final MediaRepository mediaRepository;
    private final AmazonS3 s3Client;
    private final CloudStorageConfig cloudStorageConfig;

    @Transactional
    @Override
    public Media uploadAndSaveMedia(MultipartFile file, boolean validate) throws IOException {

        if (validate) {
            validateFile(file);
        }
        System.out.println(cloudStorageConfig.getMaxSize().get("image"));
        String originalFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
        String sanitizedFilename = sanitizeFilename(originalFilename);
        String format = extractFileFormat(originalFilename);
        String contentType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");

        Media curMedia = Media.builder()
                .format(format)
                .type(contentType)
                .uploadedAt(LocalDateTime.now())
                .build();

        curMedia = saveMedia(curMedia); // Save to get mediaId for S3 key

        String key = buildS3Key(contentType, curMedia.getMediaId(), sanitizedFilename);
        curMedia.setKey(key);
        curMedia = saveMedia(curMedia); // Save again with key

        uploadToS3(file, key, contentType);

        return curMedia;
    }

    @Override
    public String getMediaUrl(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFound("Media not found"));

        return cloudStorageConfig.getCdnUrl() + "/" + media.getKey();
    }

    @Override
    public String generatePresignedUrl(String bucketName, String key) {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 5; // 5 minutes
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(bucketName, key)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(expiration);

        return s3Client.generatePresignedUrl(request).toString();
    }

    @Override
    public Media saveMedia(Media media) {
        return mediaRepository.save(media);
    }

    @Override
    public boolean existsById(UUID mediaId) {
        return mediaRepository.existsById(mediaId);
    }

    @Override
    public Optional<Media> getMediaByID(UUID mediaId) {
        return mediaRepository.findById(mediaId);
    }

    @Override
    public void deleteById(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
            .orElseThrow(() -> new EntityNotFoundException("Media not found with ID: " + mediaId));
        // Delete from S3
        s3Client.deleteObject(cloudStorageConfig.getBucketName(), media.getKey());
        // Delete from DB
        mediaRepository.deleteById(mediaId);
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
    }

    private String extractFileFormat(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return (lastDot != -1) ? filename.substring(lastDot + 1).toLowerCase() : "";
    }

    private String buildS3Key(String contentType, UUID mediaId, String filename) {
        String prefix = switch (contentType) {
            case "application/pdf" -> "doc/";
            default -> switch (contentType.split("/")[0]) {
                case "image" -> "image/";
                case "video" -> "video/";
                case "text" -> "doc/";
                default -> "misc/";
            };
        };
        return prefix + mediaId + "-" + filename;
    }

    private void uploadToS3(MultipartFile file, String key, String contentType) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(contentType);
        s3Client.putObject(cloudStorageConfig.getBucketName(), key, file.getInputStream(), metadata);
    }

    private void validateFile(MultipartFile mediaFiles) {
        if (mediaFiles.isEmpty()) {
            throw new InvalidMediaFile("Empty file not allowed.");
        }
        if (!cloudStorageConfig.getAllowedTypes().contains(mediaFiles.getContentType())) {
            throw new InvalidMediaFile("File: " + mediaFiles.getOriginalFilename() + " has invalid file type: " + mediaFiles.getContentType());
        }
        if (mediaFiles.getContentType().startsWith("image/") && mediaFiles.getSize() > cloudStorageConfig.getMaxSize().get("image")) {
            throw new InvalidMediaFile("Image: " + mediaFiles.getOriginalFilename() + " is too large. Max allowed is " + cloudStorageConfig.getMaxSize().get("image") + " MB.");

        } else if (mediaFiles.getContentType().startsWith("video/") && mediaFiles.getSize() > cloudStorageConfig.getMaxSize().get("video")) {
            throw new InvalidMediaFile("Video: " + mediaFiles.getOriginalFilename() + " is too large. Max allowed is " + cloudStorageConfig.getMaxSize().get("video") + " MB.");
        } else if (mediaFiles.getContentType().startsWith("text/") && mediaFiles.getSize() > cloudStorageConfig.getMaxSize().get("text")) {
            throw new InvalidMediaFile("File: " + mediaFiles.getOriginalFilename() + " is too large. Max allowed is " + cloudStorageConfig.getMaxSize().get("text") + " MB.");
        } else if (mediaFiles.getContentType().equals("application/pdf") && mediaFiles.getSize() > cloudStorageConfig.getMaxSize().get("application-pdf")) {
            throw new InvalidMediaFile("PDF: " + mediaFiles.getOriginalFilename() + " is too large. Max allowed is " + cloudStorageConfig.getMaxSize().get("application-pdf") + " MB.");
        }
    }
}
