package com.example.registrationmodule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.cache.annotation.EnableCaching;



@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAspectJAutoProxy
@EnableCaching
public class FriendsAndChatsModuleApplication {
    public static void main(String[] args) {
        SpringApplication.run(FriendsAndChatsModuleApplication.class, args);
    }
}
