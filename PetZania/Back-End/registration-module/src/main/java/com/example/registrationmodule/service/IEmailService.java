package com.example.registrationmodule.service;

import com.example.registrationmodule.model.dto.EmailRequestDTO;

public interface IEmailService {
    void sendEmail(EmailRequestDTO emailRequestDTO);
}
