package com.example.registrationmodule.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ResponseLoginDTO {
    private String message;
    private TokenDTO tokenDTO;
    private int loginTimes;
    private UUID userId;

}
