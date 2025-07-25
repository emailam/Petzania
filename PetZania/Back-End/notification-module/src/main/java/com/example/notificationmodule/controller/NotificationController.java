package com.example.notificationmodule.controller;

import com.example.notificationmodule.model.dto.NotificationDTO;
import com.example.notificationmodule.model.principal.UserPrincipal;
import com.example.notificationmodule.service.INotificationService;
import com.example.notificationmodule.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected Spring Bean, safe for use here")
    private final INotificationService notificationService;

    @Operation(summary = "Get notifications for a user with pagination")
    @GetMapping("")
    public ResponseEntity<Page<NotificationDTO>> getUserNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        Page<NotificationDTO> notifications = notificationService.getNotificationsByRecipient(userId, page, size, sortBy, direction);
        return notifications.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(notifications);
    }

    @Operation(summary = "Get unread notification count")
    @GetMapping("/unread-count")
    public ResponseEntity<Integer> getUnreadNotificationCount() {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        Integer count = notificationService.getUnreadNotificationCount(userId);
        return ResponseEntity.ok(count);
    }

    @Operation(summary = "Mark notification as read")
    @PutMapping("/mark-read/{notificationId}")
    public ResponseEntity<String> markAsRead(@PathVariable UUID notificationId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        boolean updated = notificationService.markAsRead(userId, notificationId);
        if (updated) {
            return ResponseEntity.ok("Notification marked as read");
        } else {
            return ResponseEntity.badRequest().body("Notification not found or already read");
        }
    }

    @Operation(summary = "Mark all notifications as read")
    @PutMapping("/mark-all-read")
    public ResponseEntity<String> markAllRead(){
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        boolean updated = notificationService.markAllRead(userId);
        if(updated){
            return ResponseEntity.ok("All notifications marked as read");
        } else {
            return ResponseEntity.badRequest().body("There are no unread notifications");
        }
    }

    @Operation(summary = "Delete a notification")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<String> deleteNotification(@PathVariable UUID notificationId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();
        boolean deleted = notificationService.deleteNotification(userId, notificationId);
        if (deleted) {
            return ResponseEntity.ok("Notification deleted successfully");
        } else {
            return ResponseEntity.badRequest().body("Notification not found");
        }
    }

}
