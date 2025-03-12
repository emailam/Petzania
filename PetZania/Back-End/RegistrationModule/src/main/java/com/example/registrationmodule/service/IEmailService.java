package com.example.registrationmodule.service;

import com.example.registrationmodule.model.entity.EmailRequest;

public interface IEmailService {
    public void sendEmail(EmailRequest emailRequest);
}
