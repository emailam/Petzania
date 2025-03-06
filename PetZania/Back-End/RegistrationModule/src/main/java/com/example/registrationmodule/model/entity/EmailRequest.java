package com.example.registrationmodule.model.entity;

import lombok.Data;

@Data
public class EmailRequest {

    private String from;
    private String to;
    private String subject;
    private String body;
}
