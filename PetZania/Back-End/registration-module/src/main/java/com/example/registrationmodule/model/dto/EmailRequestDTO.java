package com.example.registrationmodule.model.dto;

import lombok.Data;

@Data
public class EmailRequestDTO {

    private String from;
    private String to;
    private String subject;
    private String body;
}
