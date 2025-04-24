package com.example.registrationmodule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.cache.annotation.EnableCaching;



@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAspectJAutoProxy
@EnableCaching
public class RegistrationModuleApplication {
    public static void main(String[] args) {
        SpringApplication.run(RegistrationModuleApplication.class, args);
    }
}
