package com.tracker.MoneyTracker.notification;

import com.tracker.MoneyTracker.error.ErrorCode;
import com.tracker.MoneyTracker.exception.BadRequestException;
import com.tracker.MoneyTracker.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController")
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationController sut;

    @BeforeEach
    void setUp() {
        sut = new NotificationController(notificationService);
    }

    private Notification createNotification(String id, String userId, String type,
                                             String title, String message, boolean read) {
        Notification n = new Notification();
        n.setId(id);
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setRead(read);
        n.setCreatedAt(LocalDateTime.now());
        return n;
    }

    @Nested
    @DisplayName("createNotification")
    class CreateNotificationTests {

        @Test
        @DisplayName("Should create notification and return created status")
        void shouldCreate_AndReturnCreated() {
            Notification input = createNotification(null, "user-123", "GOAL_WARNING",
                    "Alert", "Msg", false);
            Notification saved = createNotification("notif-001", "user-123", "GOAL_WARNING",
                    "Alert", "Msg", false);
            given(notificationService.createNotification(any(Notification.class))).willReturn(saved);

            ResponseEntity<Notification> result = sut.createNotification(input);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getId()).isEqualTo("notif-001");
        }
    }

    @Nested
    @DisplayName("getNotifications")
    class GetNotificationsTests {

        @Test
        @DisplayName("Should return notifications for user")
        void shouldReturnNotifications_ForUser() {
            List<Notification> notifications = List.of(
                    createNotification("n1", "user-123", "GOAL_WARNING", "Alert", "Msg", false)
            );
            given(notificationService.getNotificationsByUser("user-123")).willReturn(notifications);

            ResponseEntity<List<Notification>> result = sut.getNotifications("user-123");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw BadRequestException when userId is blank")
        void shouldThrowBadRequest_WhenUserIdIsBlank() {
            assertThatThrownBy(() -> sut.getNotifications(""))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("getUnreadNotifications")
    class GetUnreadNotificationsTests {

        @Test
        @DisplayName("Should return unread notifications for user")
        void shouldReturnUnreadNotifications() {
            List<Notification> unread = List.of(
                    createNotification("n1", "user-123", "GOAL_WARNING", "Alert", "Msg", false)
            );
            given(notificationService.getUnreadNotifications("user-123")).willReturn(unread);

            ResponseEntity<List<Notification>> result = sut.getUnreadNotifications("user-123");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw BadRequestException when userId is blank")
        void shouldThrowBadRequest_WhenUserIdIsBlank() {
            assertThatThrownBy(() -> sut.getUnreadNotifications(""))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should mark notification as read and return ok")
        void shouldMarkAsRead_AndReturnOk() {
            Notification n = createNotification("notif-001", "user-123", "GOAL_WARNING",
                    "Alert", "Msg", true);
            given(notificationService.markAsRead("notif-001")).willReturn(n);

            ResponseEntity<Notification> result = sut.markAsRead("notif-001");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isRead()).isTrue();
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when notification does not exist")
        void shouldThrowNotFound_WhenDoesNotExist() {
            given(notificationService.markAsRead("nonexistent"))
                    .willThrow(new ResourceNotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND, "nonexistent"));

            assertThatThrownBy(() -> sut.markAsRead("nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("Should mark all as read and return no content")
        void shouldMarkAllAsRead_AndReturnNoContent() {
            willDoNothing().given(notificationService).markAllAsRead("user-123");

            ResponseEntity<Void> result = sut.markAllAsRead("user-123");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }

    @Nested
    @DisplayName("deleteNotification")
    class DeleteNotificationTests {

        @Test
        @DisplayName("Should delete notification and return no content")
        void shouldDelete_AndReturnNoContent() {
            willDoNothing().given(notificationService).deleteNotification("notif-001");

            ResponseEntity<Void> result = sut.deleteNotification("notif-001");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when notification does not exist")
        void shouldThrowNotFound_WhenDoesNotExist() {
            willThrow(new ResourceNotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND, "nonexistent"))
                    .given(notificationService).deleteNotification("nonexistent");

            assertThatThrownBy(() -> sut.deleteNotification("nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Should return unread count for user")
        void shouldReturnUnreadCount() {
            given(notificationService.getUnreadCount("user-123")).willReturn(3L);

            ResponseEntity<Long> result = sut.getUnreadCount("user-123");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isEqualTo(3L);
        }

        @Test
        @DisplayName("Should throw BadRequestException when userId is blank")
        void shouldThrowBadRequest_WhenUserIdIsBlank() {
            assertThatThrownBy(() -> sut.getUnreadCount(""))
                    .isInstanceOf(BadRequestException.class);
        }
    }
}
