package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.exception.authenticationAndVerificattion.EmailNotSent;
import com.example.registrationmodule.model.dto.EmailRequestDTO;
import com.example.registrationmodule.service.IEmailService;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@AllArgsConstructor
@Slf4j
public class EmailService implements IEmailService {
    private final SendGrid sendGrid;

    @Override
    public void sendEmail(EmailRequestDTO request) {
        log.info("Attempting to send email to: {}", request.getTo());
        log.info("Email subject: {}", request.getSubject());
        log.info("Email from: {}", request.getFrom());
        
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
            
            log.info("Sending request to SendGrid...");
            Response response = sendGrid.api(req);
            
            log.info("SendGrid response status: {}", response.getStatusCode());
            log.info("SendGrid response body: {}", response.getBody());
            log.info("SendGrid response headers: {}", response.getHeaders());
            
            if (response.getStatusCode() >= 400) {
                String errorMessage = "SendGrid API error: " + response.getStatusCode() + " - " + response.getBody();
                log.error(errorMessage);
                throw new EmailNotSent(errorMessage);
            }
            
            log.info("Email sent successfully to: {}", request.getTo());
        } catch (IOException e) {
            String errorMessage = "Error sending email: " + e.getMessage();
            log.error(errorMessage, e);
            throw new EmailNotSent(errorMessage);
        }
    }
}
