package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.exception.authenticationAndVerificattion.EmailNotSent;
import com.example.registrationmodule.model.dto.EmailRequestDTO;
import com.example.registrationmodule.service.IEmailService;
import lombok.AllArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EmailService implements IEmailService {
    private final JavaMailSender javaMailSender;

    @Override
    public void sendEmail(EmailRequestDTO request) {
        System.out.println("New Sending email service");
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(request.getFrom());
            message.setTo(request.getTo());
            message.setSubject(request.getSubject());
            message.setText(request.getBody());
            
            javaMailSender.send(message);
        } catch (Exception e) {
            String errorMessage = "Error sending email: " + e.getMessage();
            throw new EmailNotSent(errorMessage);
        }
    }
}
