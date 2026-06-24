package com.tracker.MoneyTracker.notification;

import com.tracker.MoneyTracker.error.ErrorCode;
import com.tracker.MoneyTracker.exception.BadRequestException;
import com.tracker.MoneyTracker.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class NotificationService {

    private static final List<String> VALID_TYPES = List.of(
            "GOAL_WARNING", "GOAL_EXCEEDED", "BUDGET_TIP", "SPENDING_INSIGHT", "SYSTEM"
    );

    private final NotificationRepository notificationRepository;

    public NotificationService() {
        this.notificationRepository = null;
    }

    @Autowired
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification createNotification(Notification notification) {
        validateNotification(notification);
        if (notification.getId() == null || notification.getId().isBlank()) {
            notification.setId(UUID.randomUUID().toString());
        }
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        return notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsByUser(String userId) {
        if (notificationRepository == null || userId == null || userId.isBlank()) {
            return List.of();
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(String userId) {
        if (notificationRepository == null || userId == null || userId.isBlank()) {
            return List.of();
        }
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    public Notification markAsRead(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND, notificationId));
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    public void markAllAsRead(String userId) {
        if (notificationRepository == null || userId == null || userId.isBlank()) {
            return;
        }
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    public void deleteNotification(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND, notificationId));
        notificationRepository.delete(notification);
    }

    public long getUnreadCount(String userId) {
        if (notificationRepository == null || userId == null || userId.isBlank()) {
            return 0L;
        }
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    private void validateNotification(Notification notification) {
        Objects.requireNonNull(notification, "Notification must not be null");
        if (notification.getUserId() == null || notification.getUserId().isBlank()) {
            throw new BadRequestException(ErrorCode.MISSING_FIELD, "userId");
        }
        if (notification.getType() == null || !VALID_TYPES.contains(notification.getType())) {
            throw new BadRequestException(ErrorCode.VALIDATION_ERROR,
                    "type must be one of: " + VALID_TYPES);
        }
        if (notification.getTitle() == null || notification.getTitle().isBlank()) {
            throw new BadRequestException(ErrorCode.MISSING_FIELD, "title");
        }
        if (notification.getMessage() == null || notification.getMessage().isBlank()) {
            throw new BadRequestException(ErrorCode.MISSING_FIELD, "message");
        }
    }
}
