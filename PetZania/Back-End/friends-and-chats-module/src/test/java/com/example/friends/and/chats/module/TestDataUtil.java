package com.example.friends.and.chats.module;

import com.example.friends.and.chats.module.model.entity.User;

public class TestDataUtil {
    public TestDataUtil() {

    }
    public static User createTestUser(String username){
        return User.builder()
                .username(username)
                .email(username + "@gmail.com")
                .build();
    }
}
