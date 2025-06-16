package com.example.registrationmodule.controller;

import com.example.registrationmodule.model.entity.Media;
import com.example.registrationmodule.service.ICloudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("api")
@Tag(name = "Cloud", description = "Endpoints for cloud management")
public class CloudController {
    private ICloudService cloudService;

    @Operation(summary = "Upload a single file to cloud storage")
    @PostMapping(path = "/cloud/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestPart(name = "file") MultipartFile file) throws IOException {
        if(!file.isEmpty()) {
            Media media = cloudService.uploadAndSaveMedia(file, true);
            return ResponseEntity.ok(cloudService.getMediaUrl(media.getMediaId()));
        }
        else {
            return ResponseEntity.ok("");
        }
    }

    @Operation(summary = "Upload multiple files to cloud storage")
    @PostMapping(path = "/cloud/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<String>> uploadFiles(@RequestPart(name = "files") List<MultipartFile> files) throws IOException {
        List<String> urls = new ArrayList<>();
        for(MultipartFile file : files) {
            if(file.isEmpty()) continue;
            Media media = cloudService.uploadAndSaveMedia(file, true);
            urls.add(cloudService.getMediaUrl(media.getMediaId()));
        }

        return ResponseEntity.ok(urls);
    }
}
