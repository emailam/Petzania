package com.example.registrationmodule;

import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.model.enumeration.UserRole;
import com.example.registrationmodule.service.IUserService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestDataUtil {
    public TestDataUtil() {
    }

    public static User createTestUserA() {
        return User.builder()
                .username("testUser")
                .password("password123")
                .email("test@example.com")
                .name("Test User")
                .bio("I love pets!")
                .profilePictureURL("http://example.com/profile.jpg")
                .phoneNumber("1234567890")
                .userRoles(List.of(UserRole.VET))
                .friends(new ArrayList<>())
                .followers(new ArrayList<>())
                .following(new ArrayList<>())
                .storeProfileId(UUID.randomUUID())
                .vetProfileId(UUID.randomUUID())
                .build();
    }
}
