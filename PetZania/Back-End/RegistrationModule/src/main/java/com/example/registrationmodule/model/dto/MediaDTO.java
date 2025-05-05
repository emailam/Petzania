package com.example.registrationmodule.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MediaDTO {
    private String key;
    private String type;
    private String format;
    private LocalDateTime uploadedAt;
}
