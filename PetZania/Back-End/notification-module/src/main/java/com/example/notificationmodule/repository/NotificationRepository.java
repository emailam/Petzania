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

    Page<Notification> findByRecipientId(UUID recipientId, Pageable pageable);

    int countByRecipientIdAndStatus(UUID recipientId, NotificationStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.status = 'READ' WHERE n.notificationId = :notificationId AND n.status = 'UNREAD'")
    int markAsRead(@Param("notificationId") UUID notificationId);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ' WHERE n.status = 'UNREAD' and n.recipientId = :ownerId")
    int markAllAsRead(@Param("ownerId") UUID ownerId);

    void deleteByNotificationId(UUID notificationId);

    void deleteByRecipientId(UUID recipientId);

    void deleteByInitiatorId(UUID initiatorId);
    void deleteByEntityId(UUID entityId);
}