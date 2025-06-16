package com.example.registrationmodule.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MediaResponseDTO {
    private UUID mediaId;
    private String key;
    private String type;
    private String format;
    private UUID postId;
    private LocalDateTime uploadedAt;
}
