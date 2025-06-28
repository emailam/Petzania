package com.example.notificationmodule.controller;

import com.example.notificationmodule.TestDataUtil;
import com.example.notificationmodule.model.entity.Notification;
import com.example.notificationmodule.model.entity.User;
import com.example.notificationmodule.model.enumeration.NotificationStatus;
import com.example.notificationmodule.model.enumeration.NotificationType;
import com.example.notificationmodule.model.principal.UserPrincipal;
import com.example.notificationmodule.repository.NotificationRepository;
import com.example.notificationmodule.repository.UserRepository;
import com.example.notificationmodule.service.IWebSocketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
@Transactional
public class NotificationControllerIntegrationTests {
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;
    private MockMvc mockMvc;
    private User testUser;
    private UserPrincipal testUserPrincipal;
    private Notification testNotification1;
    private Notification testNotification2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        testUser = userRepository.save(TestDataUtil.createTestUser("testUser"));
        setupSecurityContext(testUser);
        testNotification1 = notificationRepository.save(TestDataUtil.createTestNotification(testUser.getUserId(), NotificationType.FRIEND_REQUEST_ACCEPTED,
                "Test Notification 1", NotificationStatus.UNREAD, Instant.now().minusSeconds(3600)));
        testNotification2 = notificationRepository.save(TestDataUtil.createTestNotification(testUser.getUserId(), NotificationType.PET_POST_LIKED,
                "Test Notification 2", NotificationStatus.READ, Instant.now().minusSeconds(1800)));
    }

    @Test
    void testGetUserNotifications_DefaultPagination() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.content[0].message", is("Test Notification 2")))// Most recent first
                .andExpect(jsonPath("$.content[0].recipientId", is(testUser.getUserId().toString())))
                .andExpect(jsonPath("$.content[0].type", is(NotificationType.PET_POST_LIKED.toString())))
                .andExpect(jsonPath("$.content[0].status", is(NotificationStatus.READ.toString())))
                .andExpect(jsonPath("$.content[0].createdAt", is(testNotification2.getCreatedAt().toString())))
                .andExpect(jsonPath("$.content[1].message", is("Test Notification 1")));
    }

    @Test
    void testGetUserNotifications_CustomPagination() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sortBy", "createdAt")
                        .param("direction", "asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.size", is(1)))
                .andExpect(jsonPath("$.content[0].message", is("Test Notification 1"))); // Oldest first
    }

    @Test
    void testGetUserNotifications_NoNotifications() throws Exception {
        // Delete all notifications
        notificationRepository.deleteAll();

        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    void testGetUnreadNotificationCount_Success() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("1")); // Only testNotification1 is unread
    }

    @Test
    void testGetUserNotifications_SortByStatus() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .param("sortBy", "status")
                        .param("direction", "asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status", is("READ")))
                .andExpect(jsonPath("$.content[1].status", is("UNREAD")));
    }

    @Test
    void testGetUserNotifications_LargePageSize() throws Exception {
        // Create many notifications
        IntStream.range(0, 50).forEach(i -> {
            notificationRepository.save(TestDataUtil.createTestNotification(
                    testUser.getUserId(),
                    NotificationType.NEW_FOLLOWER,
                    "Notification " + i,
                    NotificationStatus.UNREAD,
                    Instant.now().minusSeconds(i * 60)
            ));
        });

        mockMvc.perform(get("/api/notifications")
                        .param("size", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(52))) // 50 + 2 original
                .andExpect(jsonPath("$.size", is(100)));
    }

    @Test
    void testNoAuthentication() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("No authenticated user found")));
    }



    @Test
    void testGetUnreadNotificationCount_Zero() throws Exception {
        // Mark all as read
        mockMvc.perform(put("/api/notifications/mark-all-read")
                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications/unread-count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    void testGetUnreadNotificationCount_Multiple() throws Exception {
        // Add more unread notifications
        IntStream.range(0, 5).forEach(i -> {
            notificationRepository.save(TestDataUtil.createTestNotification(
                    testUser.getUserId(),
                    NotificationType.NEW_FOLLOWER,
                    "Unread " + i,
                    NotificationStatus.UNREAD,
                    Instant.now()
            ));
        });

        mockMvc.perform(get("/api/notifications/unread-count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("6")); // 1 original + 5 new
    }


    @Test
    void testMarkAsRead_Success() throws Exception {
        mockMvc.perform(put("/api/notifications/mark-read/{notificationId}", testNotification1.getNotificationId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Notification marked as read"));

        // Verify notification was marked as read
        Notification updatedNotification = notificationRepository.findById(testNotification1.getNotificationId()).orElseThrow();
        assertEquals(NotificationStatus.READ, updatedNotification.getStatus());
    }

    @Test
    void testMarkAsRead_NotificationNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(put("/api/notifications/mark-read/{notificationId}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void testMarkAsRead_UnauthorizedAccess() throws Exception {
        // Create another user and notification
        User anotherUser = User.builder()
                .userId(UUID.randomUUID())
                .username("anotheruser")
                .email("another@example.com")
                .build();
        anotherUser = userRepository.save(anotherUser);

        Notification anotherNotification = Notification.builder()
                .recipientId(anotherUser.getUserId())
                .type(NotificationType.FRIEND_REQUEST_RECEIVED)
                .message("Another user's notification")
                .status(NotificationStatus.UNREAD)
                .build();
        anotherNotification = notificationRepository.save(anotherNotification);

        mockMvc.perform(put("/api/notifications/mark-read/{notificationId}", anotherNotification.getNotificationId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Notification not found or already read"));
    }

    @Test
    void testMarkAllRead_Success() throws Exception {
        mockMvc.perform(put("/api/notifications/mark-all-read")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("All notifications marked as read"));

        // Verify all notifications are read
        long unreadCount = notificationRepository.countByRecipientIdAndStatus(testUser.getUserId(), NotificationStatus.UNREAD);
        assertEquals(0, unreadCount);
    }

    @Test
    void testMarkAllRead_NoUnreadNotifications() throws Exception {
        // First mark all as read
        notificationRepository.markAllAsRead(testUser.getUserId());

        mockMvc.perform(put("/api/notifications/mark-all-read")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string("There are no unread notifications"));
    }

    @Test
    void testDeleteNotification_Success() throws Exception {
        mockMvc.perform(delete("/api/notifications/{notificationId}", testNotification1.getNotificationId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Notification deleted successfully"));

        // Verify notification was deleted
        assertFalse(notificationRepository.existsById(testNotification1.getNotificationId()));
    }

    @Test
    void testDeleteNotification_NotificationNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/notifications/{notificationId}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteNotification_UnauthorizedAccess() throws Exception {
        // Create another user and notification
        User anotherUser = User.builder()
                .userId(UUID.randomUUID())
                .username("anotheruser2")
                .email("another2@example.com")
                .build();
        anotherUser = userRepository.save(anotherUser);

        Notification anotherNotification = Notification.builder()
                .recipientId(anotherUser.getUserId())
                .type(NotificationType.NEW_FOLLOWER)
                .message("Another user's notification for deletion test")
                .status(NotificationStatus.UNREAD)
                .build();
        anotherNotification = notificationRepository.save(anotherNotification);

        mockMvc.perform(delete("/api/notifications/{notificationId}", anotherNotification.getNotificationId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Notification not found"));
    }

    @Test
    void testInvalidUUIDFormat() throws Exception {
        mockMvc.perform(put("/api/notifications/mark-read/invalid-uuid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testNegativePaginationParameters() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .param("page", "-1")
                        .param("size", "-5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInvalidSortDirection() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .param("direction", "invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk()) // Should default to desc
                .andExpect(jsonPath("$.content[0].message", is("Test Notification 1"))); // Most recent first
    }

    @Test
    private void setupSecurityContext(User user) {
        UserPrincipal userPrincipal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userPrincipal,
                        null,
                        userPrincipal.getAuthorities()
                )
        );
    }

}
