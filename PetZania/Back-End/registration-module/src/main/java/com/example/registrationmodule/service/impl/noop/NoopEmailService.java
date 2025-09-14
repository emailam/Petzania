package com.example.registrationmodule.service.impl.noop;

import com.example.registrationmodule.model.dto.EmailRequestDTO;
import com.example.registrationmodule.service.IEmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
@Slf4j
public class NoopEmailService  implements IEmailService {
    @Override
    public void sendEmail(EmailRequestDTO request) {
        log.info("FAKE EMAIL SERVICE: Would send email to '{}', subject: '{}'", request.getTo(), request.getSubject());
        // Do nothing else!
    }
}
