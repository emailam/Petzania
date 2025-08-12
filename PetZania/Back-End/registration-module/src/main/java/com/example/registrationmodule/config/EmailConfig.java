package com.example.registrationmodule.config;

import com.sendgrid.SendGrid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
//@PropertySource("classpath:application.yml")
@Slf4j
public class EmailConfig {

    @Value("${spring.sendgrid.key}")
    private String key;
    
    @Bean
    public SendGrid getSendgrid(){
        if (key == null || key.trim().isEmpty()) {
            log.error("SendGrid API key is null or empty!");
            throw new IllegalArgumentException("SendGrid API key is required");
        }
        
        if (!key.startsWith("SG.")) {
            log.error("SendGrid API key format is invalid. Should start with 'SG.'");
            throw new IllegalArgumentException("Invalid SendGrid API key format");
        }
        
        log.info("Initializing SendGrid with API key: {}", key.substring(0, 10) + "...");
        return new SendGrid(key);
    }
}
