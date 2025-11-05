package com.example.registrationmodule.service.noop;

import com.example.registrationmodule.model.entity.Media;
import com.example.registrationmodule.service.impl.noop.NoopCloudService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NoopCloudServiceTest {

    private NoopCloudService noopCloudService;
    private MultipartFile multipartFile;

    @BeforeEach
    void setUp() {
        noopCloudService = new NoopCloudService();
        multipartFile = mock(MultipartFile.class);
    }

    @Test
    void uploadAndSaveMedia_ReturnsFakeMedia() throws Exception {
        when(multipartFile.getOriginalFilename()).thenReturn("testfile.txt");
        when(multipartFile.getSize()).thenReturn(42L);

        Media media = noopCloudService.uploadAndSaveMedia(multipartFile, true);

        assertNotNull(media);
        assertEquals("noop", media.getFormat());
        assertEquals("noop", media.getType());
        assertTrue(media.getKey().contains("noop/testfile.txt"));
        assertNotNull(media.getUploadedAt());
        assertNotNull(media.getMediaId());
    }

    @Test
    void getMediaUrl_ReturnsFakeUrl() {
        UUID id = UUID.randomUUID();
        String url = noopCloudService.getMediaUrl(id);
        assertEquals("http://localhost:8080/noop-media/" + id, url);
    }

    @Test
    void generatePresignedUrl_ReturnsFakeUrl() {
        String url = noopCloudService.generatePresignedUrl("anyBucket", "anyKey.mp4");
        assertEquals("http://localhost:8080/noop-presigned/anyKey.mp4", url);
    }

    @Test
    void saveMedia_ReturnsSameMedia() {
        Media m = Media.builder().mediaId(UUID.randomUUID()).key("k").build();
        Media returned = noopCloudService.saveMedia(m);
        assertSame(m, returned);
    }

    @Test
    void existsById_AlwaysTrue() {
        assertTrue(noopCloudService.existsById(UUID.randomUUID()));
    }

    @Test
    void getMediaByID_ReturnsFakeMedia() {
        UUID id = UUID.randomUUID();
        Optional<Media> optionalMedia = noopCloudService.getMediaByID(id);

        assertTrue(optionalMedia.isPresent());
        Media fake = optionalMedia.get();

        assertEquals(id, fake.getMediaId());
        assertEquals("noop", fake.getFormat());
        assertEquals("noop", fake.getType());
        assertTrue(fake.getKey().contains("noop/"));
        assertNotNull(fake.getUploadedAt());
    }

    @Test
    void deleteById_DoesNothing() {
        UUID id = UUID.randomUUID();
        // Should not throw
        noopCloudService.deleteById(id);
    }
}
