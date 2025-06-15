package com.example.friends.and.chats.module;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableAspectJAutoProxy
public class FriendsAndChatsModuleApplication {

	public static void main(String[] args) {
		SpringApplication.run(FriendsAndChatsModuleApplication.class, args);
	}

}
