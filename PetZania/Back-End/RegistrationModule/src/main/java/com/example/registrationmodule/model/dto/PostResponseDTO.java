package com.example.registrationmodule.model.dto;

import com.example.registrationmodule.model.enumeration.Mood;
import com.example.registrationmodule.model.enumeration.Visibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostResponseDTO {
    private UUID postId;
    @NotNull
    @Size(max = 1000)
    private String caption;
    @Size(max = 10)
    private List<MediaResponseDTO> mediaList;
    private Mood mood;
    private Visibility visibility;
    private LocalDateTime createdAt;
    private UUID userId;
}

