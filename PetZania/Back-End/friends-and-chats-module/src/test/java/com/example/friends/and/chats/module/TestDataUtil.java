package com.example.friends.and.chats.module;

import com.example.friends.and.chats.module.model.entity.User;

import java.util.UUID;

public class TestDataUtil {
    public TestDataUtil() {

    }
    public static User createTestUser(String username){
        return User.builder()
                .userId(UUID.randomUUID())
                .username(username)
                .email(username + "@gmail.com")
                .build();
    }
}
