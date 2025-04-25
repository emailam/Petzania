package com.example.registrationmodule.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResponseLoginDTO {
    private String message;
    private TokenDTO tokenDTO;

}
