package com.example.registrationmodule.controller;

import com.example.registrationmodule.model.entity.Media;
import com.example.registrationmodule.service.ICloudService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@AllArgsConstructor
@RequestMapping("api/post")
@Validated
public class PostController {
    private final ICloudService cloudService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadMedia(@RequestPart(value = "mediaFile", required = false)  MultipartFile mediaFiles) throws IOException {
        Media media = cloudService.uploadAndSaveMedia(mediaFiles, true);
        return ResponseEntity.ok(cloudService.getMediaUrl(media.getMediaId()));
    }

}
