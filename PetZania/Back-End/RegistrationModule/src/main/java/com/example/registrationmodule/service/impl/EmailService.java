package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.exception.EmailNotSent;
import com.example.registrationmodule.model.entity.EmailRequest;
import com.example.registrationmodule.service.IEmailService;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService implements IEmailService {

    @Override
    public void sendEmail(EmailRequest request) {
        String key = "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";
        Email from = new Email(request.getFrom());
        Email to = new Email(request.getTo());
        Content emailContent = new Content("text/plain", request.getBody());
        String subject = request.getSubject();
        Mail mail = new Mail(from, subject, to, emailContent);
        mail.addHeader("X-Priority", "1");
        SendGrid sendGrid = new SendGrid(key);
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
