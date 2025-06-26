package com.example.notificationmodule.repository;


import com.example.notificationmodule.model.entity.Notification;
import com.example.notificationmodule.model.enumeration.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Get notifications with pagination
    Page<Notification> findByRecipientId(UUID recipientId, Pageable pageable);

    // Count unread notifications
    long countByRecipientId(UUID recipientId);

    // Update notification status from UNREAD to READ
    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ' WHERE n.notificationId = :notificationId AND n.status = 'UNREAD'")
    int markAsRead(@Param("notificationId") UUID notificationId);

    // Delete notification
    void deleteByNotificationId(UUID notificationId);
}