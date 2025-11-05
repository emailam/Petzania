package com.example.petzaniasystemtests.builders;

import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class TestDataBuilder {
    public static class UserBuilder {
        public static String createRegisterUserJson(String username, String email, String password) {
            return String.format("""
                    {
                        "username": "%s",
                        "email": "%s",
                        "password": "%s"
                    }
                    """, username, email, password);
        }

        public enum NotificationType {
            FRIEND_REQUEST_RECEIVED,
            FRIEND_REQUEST_ACCEPTED,
            NEW_FOLLOWER,
            PET_POST_LIKED
        }

        public enum NotificationStatus {
            READ,
            UNREAD
        }

        public static String createLoginJson(String email, String password) {
            return String.format("""
                    {
                        "email": "%s",
                        "password": "%s"
                    }
                    """, email, password);
        }

        public static String createOtpValidationJson(String email, String otp) {
            return String.format("""
                    {
                        "email": "%s",
                        "otp": "%s"
                    }
                    """, email, otp);
        }

        public static String createUpdateProfileJson(String name, String bio, String phoneNumber) {
            return String.format("""
                    {
                        "name": "%s",
                        "bio": "%s",
                        "phoneNumber": "%s"
                    }
                    """, name, bio, phoneNumber);
        }

        public static String createBlockUserJson(String email) {
            return String.format("""
                    {
                        "email": "%s"
                    }
                    """, email);
        }
    }

    public static class PetBuilder {
        public static String createPetJson(String name, String species, String gender, String breed) {
            return String.format("""
                    {
                        "name": "%s",
                        "species": "%s",
                        "gender": "%s",
                        "breed": "%s",
                        "dateOfBirth": "2020-01-01",
                        "description": "Test pet"
                    }
                    """, name, species, gender, breed);
        }
    }

    public static class MessageBuilder {
        public static String createSendMessageJson(UUID chatId, String content) {
            return String.format("""
                    {
                        "chatId": "%s",
                        "content": "%s",
                        "isFile": false
                    }
                    """, chatId, content);
        }

        public static String createUpdateMessageContentJson(String content) {
            return String.format("""
                    {
                        "content": "%s"
                    }
                    """, content);
        }

        public static String createUpdateMessageStatusJson(String status) {
            return String.format("""
                    {
                        "messageStatus": "%s"
                    }
                    """, status);
        }

        public static String createMessageReactionJson(String reaction) {
            return String.format("""
                    {
                        "messageReact": "%s"
                    }
                    """, reaction);
        }
    }

    public static class ChatBuilder {
        public static String createUpdateUserChatJson(boolean pinned, boolean muted) {
            return String.format("""
                    {
                        "pinned": %b,
                        "muted": %b,
                        "unread": 0
                    }
                    """, pinned, muted);
        }
    }
}
