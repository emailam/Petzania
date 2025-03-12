package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.exception.EmailNotSent;
import com.example.registrationmodule.model.dto.EmailRequestDTO;
import com.example.registrationmodule.service.IEmailService;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@AllArgsConstructor
public class EmailService implements IEmailService {
    private final SendGrid sendGrid;

    @Override
    public void sendEmail(EmailRequestDTO request) {
        Email from = new Email(request.getFrom());
        Email to = new Email(request.getTo());
        Content emailContent = new Content("text/plain", request.getBody());
        String subject = request.getSubject();
        Mail mail = new Mail(from, subject, to, emailContent);
        mail.addHeader("X-Priority", "1");
        Request req = new Request();
        try {
            req.setMethod(Method.POST);
            req.setEndpoint("mail/send");
            req.setBody(mail.build());
            sendGrid.api(req);
        } catch (IOException e) {
            String errorMessage = "Error sending email: " + e.getMessage();
            throw new EmailNotSent(errorMessage);
        }
    }
}
