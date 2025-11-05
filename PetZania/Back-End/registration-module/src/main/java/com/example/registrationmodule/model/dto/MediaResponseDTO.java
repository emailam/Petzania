package com.example.registrationmodule.model.dto;

import jakarta.validation.constraints.Size;
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
    @Size(max = 255, message = "Key must not exceed 255 characters.")
    private String key;
    @Size(max = 50, message = "Type must not exceed 50 characters.")
    private String type;
    @Size(max = 10, message = "Format must not exceed 10 characters.")
    private String format;
    private UUID postId;
    private LocalDateTime uploadedAt;
}
