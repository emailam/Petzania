package com.example.registrationmodule.service;

import com.example.registrationmodule.model.entity.Media;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public interface ICloudService {

//    List<Media> uploadAndSaveMedia(List<MultipartFile> files, boolean validate) throws IOException;
    Media uploadAndSaveMedia(MultipartFile files, boolean validate) throws IOException;

    String getMediaUrl(UUID mediaId);

    String generatePresignedUrl(String bucketName, String key);

    Media saveMedia(Media media);

    boolean existsById(UUID mediaId);

    Optional<Media> getMediaByID(UUID mediaId);

    void deleteById(UUID mediaId);
}
