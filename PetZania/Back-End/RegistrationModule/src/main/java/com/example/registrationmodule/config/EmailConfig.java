package com.example.registrationmodule.config;

import com.sendgrid.SendGrid;
import lombok.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailConfig {


    @Bean
    public SendGrid getSendgrid(){
        String key = "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";
        return new SendGrid(key);
    }

}
