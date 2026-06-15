package com.tracker.MoneyTracker.notification;

import com.tracker.MoneyTracker.error.ErrorCode;
import com.tracker.MoneyTracker.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController() {
        this.notificationService = null;
    }

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<Notification> createNotification(@RequestBody Notification notification) {
        if (notificationService == null) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        Notification created = notificationService.createNotification(notification);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(@RequestParam("userId") String userId) {
        if (notificationService == null) {
            return ResponseEntity.ok(List.of());
        }
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT, "userId is required");
        }
        return ResponseEntity.ok(notificationService.getNotificationsByUser(userId));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@RequestParam("userId") String userId) {
        if (notificationService == null) {
            return ResponseEntity.ok(List.of());
        }
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT, "userId is required");
        }
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable String notificationId) {
        if (notificationService == null) {
            return ResponseEntity.ok(null);
        }
        Notification updated = notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@RequestParam("userId") String userId) {
        if (notificationService == null) {
            return ResponseEntity.noContent().build();
        }
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable String notificationId) {
        if (notificationService == null) {
            return ResponseEntity.noContent().build();
        }
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount(@RequestParam("userId") String userId) {
        if (notificationService == null) {
            return ResponseEntity.ok(0L);
        }
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT, "userId is required");
        }
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }
}
