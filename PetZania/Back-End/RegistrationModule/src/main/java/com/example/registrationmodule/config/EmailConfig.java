package com.example.registrationmodule.config;

import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailConfig {
    @Value("${spring.sendgrid.key}")
    private String key = "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";
    @Bean
    public SendGrid getSendgrid(){
        return new SendGrid(key);
    }

}
