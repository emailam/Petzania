package com.example.registrationmodule.service.noop;


import com.example.registrationmodule.model.dto.EmailRequestDTO;
import com.example.registrationmodule.service.impl.noop.NoopEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NoopEmailServiceTest {

    private NoopEmailService noopEmailService;

    @BeforeEach
    void setUp() {
        noopEmailService = new NoopEmailService();
    }

    @Test
    void sendEmail_DoesNotThrow() {
        EmailRequestDTO request = new EmailRequestDTO();
        request.setTo("to@example.com");
        request.setSubject("Test Subject");
        request.setBody("Test Body");

        // Method should not throw any exceptions
        noopEmailService.sendEmail(request);
    }
}
