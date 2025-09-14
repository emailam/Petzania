package com.example.registrationmodule.service.impl.noop;


import com.example.registrationmodule.model.entity.Media;
import com.example.registrationmodule.service.ICloudService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
@Slf4j
public class NoopCloudService implements ICloudService {
    @Transactional
    @Override
    public Media uploadAndSaveMedia(MultipartFile file, boolean validate) {
        log.info("[NOOP CLOUD] Pretending to upload file: '{}' (size: {})", file.getOriginalFilename(), file.getSize());
        // Return a dummy Media object as if uploaded
        return Media.builder()
                .mediaId(UUID.randomUUID())
                .format("noop")
                .type("noop")
                .uploadedAt(LocalDateTime.now())
                .key("noop/" + file.getOriginalFilename())
                .build();
    }

    @Override
    public String getMediaUrl(UUID mediaId) {
        String fakeUrl = "http://localhost:8080/noop-media/" + mediaId;
        log.info("[NOOP CLOUD] Returning fake media URL: {}", fakeUrl);
        return fakeUrl;
    }

    @Override
    public String generatePresignedUrl(String bucketName, String key) {
        String fakePresigned = "http://localhost:8080/noop-presigned/" + key;
        log.info("[NOOP CLOUD] Returning fake presigned URL: {}", fakePresigned);
        return fakePresigned;
    }

    @Override
    public Media saveMedia(Media media) {
        log.info("[NOOP CLOUD] Fake saving media: {}", media);
        return media;
    }

    @Override
    public boolean existsById(UUID mediaId) {
        log.info("[NOOP CLOUD] Always returns true for existsById({})", mediaId);
        return true;
    }

    @Override
    public Optional<Media> getMediaByID(UUID mediaId) {
        log.info("[NOOP CLOUD] Always returns a fake Media for getMediaByID({})", mediaId);
        Media fakeMedia = Media.builder()
                .mediaId(mediaId)
                .format("noop")
                .type("noop")
                .uploadedAt(LocalDateTime.now())
                .key("noop/" + mediaId)
                .build();
        return Optional.of(fakeMedia);
    }

    @Override
    public void deleteById(UUID mediaId) {
        log.info("[NOOP CLOUD] Pretending to delete media with id: {}", mediaId);
    }
}
