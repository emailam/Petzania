package com.example.registrationmodule.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.example.registrationmodule.config.CloudStorageConfig;
import com.example.registrationmodule.exception.media.InvalidMediaFile;
import com.example.registrationmodule.exception.media.MediaNotFound;
import com.example.registrationmodule.exception.rateLimiting.TooManyCloudRequests;
import com.example.registrationmodule.model.entity.Media;
import com.example.registrationmodule.repository.MediaRepository;
import com.example.registrationmodule.service.impl.CloudService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import jakarta.persistence.EntityNotFoundException;

class CloudServiceTest {
    @Mock private MediaRepository mediaRepository;
    @Mock private AmazonS3 s3Client;
    @Mock private CloudStorageConfig cloudStorageConfig;
    @InjectMocks private CloudService cloudService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void uploadAndSaveMedia_success() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
        Media media = Media.builder().mediaId(UUID.randomUUID()).build();
        when(mediaRepository.save(any())).thenReturn(media);
        when(cloudStorageConfig.getMaxSize()).thenReturn(Map.of("image", 10000000L));
        when(cloudStorageConfig.getAllowedTypes()).thenReturn(List.of("image/jpeg"));
        when(cloudStorageConfig.getBucketName()).thenReturn("bucket");
        Media result = cloudService.uploadAndSaveMedia(file, true);
        assertNotNull(result);
    }

    @Test
    void uploadAndSaveMedia_invalidFile_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);
        when(cloudStorageConfig.getAllowedTypes()).thenReturn(List.of("image/jpeg"));
        when(cloudStorageConfig.getMaxSize()).thenReturn(Map.of("image", 10000000L));
        assertThrows(InvalidMediaFile.class, () -> cloudService.uploadAndSaveMedia(file, true));
    }

    @Test
    void uploadAndSaveMedia_fallback_throws() {
        assertThrows(TooManyCloudRequests.class, () -> cloudService.uploadFallback(null, true, mock(RequestNotPermitted.class)));
    }

    @Test
    void getMediaUrl_success() {
        UUID id = UUID.randomUUID();
        Media media = Media.builder().mediaId(id).key("key").build();
        when(mediaRepository.findById(id)).thenReturn(Optional.of(media));
        when(cloudStorageConfig.getCdnUrl()).thenReturn("http://cdn");
        String url = cloudService.getMediaUrl(id);
        assertEquals("http://cdn/key", url);
    }

    @Test
    void getMediaUrl_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(mediaRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(MediaNotFound.class, () -> cloudService.getMediaUrl(id));
    }

    @Test
    void getMediaUrl_fallback_throws() {
        assertThrows(TooManyCloudRequests.class, () -> cloudService.mediaUrlFallback(UUID.randomUUID(), mock(RequestNotPermitted.class)));
    }

    @Test
    void generatePresignedUrl_success() throws java.net.MalformedURLException {
        String url = "http://presigned";
        when(s3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(new java.net.URL("http://presigned"));
        String result = cloudService.generatePresignedUrl("bucket", "key");
        assertEquals(url, result);
    }

    @Test
    void saveMedia_success() {
        Media media = Media.builder().mediaId(UUID.randomUUID()).build();
        when(mediaRepository.save(media)).thenReturn(media);
        assertEquals(media, cloudService.saveMedia(media));
    }

    @Test
    void existsById_true() {
        UUID id = UUID.randomUUID();
        when(mediaRepository.existsById(id)).thenReturn(true);
        assertTrue(cloudService.existsById(id));
    }

    @Test
    void existsById_false() {
        UUID id = UUID.randomUUID();
        when(mediaRepository.existsById(id)).thenReturn(false);
        assertFalse(cloudService.existsById(id));
    }

    @Test
    void getMediaByID_found() {
        UUID id = UUID.randomUUID();
        Media media = Media.builder().mediaId(id).build();
        when(mediaRepository.findById(id)).thenReturn(Optional.of(media));
        assertEquals(Optional.of(media), cloudService.getMediaByID(id));
    }

    @Test
    void getMediaByID_notFound() {
        UUID id = UUID.randomUUID();
        when(mediaRepository.findById(id)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), cloudService.getMediaByID(id));
    }

    @Test
    void deleteById_success() {
        UUID id = UUID.randomUUID();
        Media media = Media.builder().mediaId(id).key("key").build();
        when(mediaRepository.findById(id)).thenReturn(Optional.of(media));
        when(cloudStorageConfig.getBucketName()).thenReturn("bucket");
        doNothing().when(s3Client).deleteObject("bucket", "key");
        doNothing().when(mediaRepository).deleteById(id);
        assertDoesNotThrow(() -> cloudService.deleteById(id));
    }

    @Test
    void deleteById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(mediaRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> cloudService.deleteById(id));
    }
} 