package com.example.registrationmodule.controller;

import com.example.registrationmodule.model.entity.Media;
import com.example.registrationmodule.service.ICloudService;
import lombok.AllArgsConstructor;
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
public class CloudController {
    private ICloudService cloudService;

    @PostMapping(path = "/cloud/file")
    public ResponseEntity<String> uploadFile(@RequestPart(name = "file") MultipartFile file) throws IOException {
        if(!file.isEmpty()) {
            Media media = cloudService.uploadAndSaveMedia(file, true);
            return ResponseEntity.ok(cloudService.getMediaUrl(media.getMediaId()));
        }
        else {
            return ResponseEntity.ok("");
        }
    }

    @PostMapping(path = "/cloud/files")
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
