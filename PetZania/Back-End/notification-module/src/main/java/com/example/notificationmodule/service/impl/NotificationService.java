package com.example.notificationmodule.service.impl;

import com.example.notificationmodule.exception.notification.NotificationNotFound;
import com.example.notificationmodule.exception.user.UserNotFound;
import com.example.notificationmodule.model.dto.NotificationDTO;
import com.example.notificationmodule.model.entity.Notification;
import com.example.notificationmodule.model.enumeration.NotificationStatus;
import com.example.notificationmodule.repository.NotificationRepository;
import com.example.notificationmodule.repository.UserRepository;
import com.example.notificationmodule.service.INotificationService;
import com.example.notificationmodule.service.IDTOConversionService;
import com.example.notificationmodule.service.IWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationService implements INotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final IDTOConversionService dtoConversionService;
    private final IWebSocketService webSocketService;


    public void isUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFound("User does not Exist");
        }
        // user exists baby don't worry
    }

    @Override
    public Page<NotificationDTO> getNotificationsByRecipient(UUID recipientId, int page, int size, String sortBy, String direction) {
        log.info("Getting notifications for recipient: {}", recipientId);
        isUserExists(recipientId);
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return notificationRepository.findByRecipientId(recipientId, pageable).map(dtoConversionService::toDTO);
    }

    @Override
    public int getUnreadNotificationCount(UUID recipientId) {
        log.info("Getting unread notification count for recipient: {}", recipientId);
        isUserExists(recipientId);
        return notificationRepository.countByRecipientIdAndStatus(recipientId, NotificationStatus.UNREAD);
    }

    @Override
    @Transactional
    public boolean markAsRead(UUID ownerId, UUID notificationId) {
        log.info("Marking notification as read: {}", notificationId);
        isUserExists(ownerId);
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(() -> new NotificationNotFound("Notification does not exist"));
        if (notification.getRecipientId().equals(ownerId)) {
            int updatedRows = notificationRepository.markAsRead(notificationId);
            if (updatedRows > 0) {
                int newCount = getUnreadNotificationCount(ownerId);
                webSocketService.sendNotificationCountUpdate(ownerId, newCount);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean markAllRead(UUID ownerId) {
        log.info("Marking all notifications as read for userId: {}", ownerId);
        isUserExists(ownerId);
        int updatedRows = notificationRepository.markAllAsRead(ownerId);
        if (updatedRows > 0) {
            int newCount = getUnreadNotificationCount(ownerId);
            webSocketService.sendNotificationCountUpdate(ownerId, newCount);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean deleteNotification(UUID ownerId, UUID notificationId) {
        log.info("Deleting notification: {}", notificationId);
        isUserExists(ownerId);
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(() -> new NotificationNotFound("Notification does not exist"));
        if (notification.getRecipientId().equals(ownerId)) {
            notificationRepository.deleteByNotificationId(notificationId);
            int newCount = getUnreadNotificationCount(ownerId);
            webSocketService.sendNotificationCountUpdate(ownerId, newCount);
            return true;
        } else {
            return false;
        }
    }
}
