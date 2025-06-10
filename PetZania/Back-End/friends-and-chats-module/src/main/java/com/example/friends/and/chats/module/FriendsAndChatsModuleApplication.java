package com.example.friends.and.chats.module;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class FriendsAndChatsModuleApplication {

	public static void main(String[] args) {
		SpringApplication.run(FriendsAndChatsModuleApplication.class, args);
	}

}
