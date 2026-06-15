package com.tracker.MoneyTracker.notification;

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
            // Arrange
            Notification input = createNotification(null, "user-123", "GOAL_WARNING",
                    "Alert", "Msg", false);
            Notification saved = createNotification("notif-001", "user-123", "GOAL_WARNING",
                    "Alert", "Msg", false);
            given(notificationService.createNotification(any(Notification.class))).willReturn(saved);

            // Act
            ResponseEntity<Notification> result = sut.createNotification(input);

            // Assert
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
            // Arrange
            List<Notification> notifications = List.of(
                    createNotification("n1", "user-123", "GOAL_WARNING", "Alert", "Msg", false)
            );
            given(notificationService.getNotificationsByUser("user-123")).willReturn(notifications);

            // Act
            ResponseEntity<List<Notification>> result = sut.getNotifications("user-123");

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).hasSize(1);
        }

        @Test
        @DisplayName("Should return bad request when userId is blank")
        void shouldReturnBadRequest_WhenUserIdIsBlank() {
            ResponseEntity<List<Notification>> result = sut.getNotifications("");
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("getUnreadNotifications")
    class GetUnreadNotificationsTests {

        @Test
        @DisplayName("Should return unread notifications for user")
        void shouldReturnUnreadNotifications() {
            // Arrange
            List<Notification> unread = List.of(
                    createNotification("n1", "user-123", "GOAL_WARNING", "Alert", "Msg", false)
            );
            given(notificationService.getUnreadNotifications("user-123")).willReturn(unread);

            // Act
            ResponseEntity<List<Notification>> result = sut.getUnreadNotifications("user-123");

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).hasSize(1);
        }

        @Test
        @DisplayName("Should return bad request when userId is blank")
        void shouldReturnBadRequest_WhenUserIdIsBlank() {
            ResponseEntity<List<Notification>> result = sut.getUnreadNotifications("");
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should mark notification as read and return ok")
        void shouldMarkAsRead_AndReturnOk() {
            // Arrange
            Notification n = createNotification("notif-001", "user-123", "GOAL_WARNING",
                    "Alert", "Msg", true);
            given(notificationService.markAsRead("notif-001")).willReturn(n);

            // Act
            ResponseEntity<Notification> result = sut.markAsRead("notif-001");

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isRead()).isTrue();
        }

        @Test
        @DisplayName("Should return not found when notification does not exist")
        void shouldReturnNotFound_WhenDoesNotExist() {
            given(notificationService.markAsRead("nonexistent"))
                    .willThrow(new IllegalArgumentException("Notification not found"));

            ResponseEntity<Notification> result = sut.markAsRead("nonexistent");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("Should mark all as read and return no content")
        void shouldMarkAllAsRead_AndReturnNoContent() {
            // Arrange
            willDoNothing().given(notificationService).markAllAsRead("user-123");

            // Act
            ResponseEntity<Void> result = sut.markAllAsRead("user-123");

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }

    @Nested
    @DisplayName("deleteNotification")
    class DeleteNotificationTests {

        @Test
        @DisplayName("Should delete notification and return no content")
        void shouldDelete_AndReturnNoContent() {
            // Arrange
            willDoNothing().given(notificationService).deleteNotification("notif-001");

            // Act
            ResponseEntity<Void> result = sut.deleteNotification("notif-001");

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("Should return not found when notification does not exist")
        void shouldReturnNotFound_WhenDoesNotExist() {
            willThrow(new IllegalArgumentException("Notification not found"))
                    .given(notificationService).deleteNotification("nonexistent");

            ResponseEntity<Void> result = sut.deleteNotification("nonexistent");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Should return unread count for user")
        void shouldReturnUnreadCount() {
            // Arrange
            given(notificationService.getUnreadCount("user-123")).willReturn(3L);

            // Act
            ResponseEntity<Long> result = sut.getUnreadCount("user-123");

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isEqualTo(3L);
        }

        @Test
        @DisplayName("Should return bad request when userId is blank")
        void shouldReturnBadRequest_WhenUserIdIsBlank() {
            ResponseEntity<Long> result = sut.getUnreadCount("");
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
