package com.tracker.MoneyTracker.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.tracker.MoneyTracker.exception.BadRequestException;
import com.tracker.MoneyTracker.exception.ResourceNotFoundException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService sut;

    @BeforeEach
    void setUp() {
        sut = new NotificationService(notificationRepository);
    }

    private Notification createNotification(String id, String userId, String type, String title,
                                             String message, boolean read) {
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
        @DisplayName("Should create and save a valid notification")
        void shouldCreateAndSave_WhenValid() {
            // Arrange
            Notification input = createNotification(null, "user-123", "GOAL_WARNING",
                    "Budget Alert", "You are close to your FOOD budget limit", false);
            given(notificationRepository.save(any(Notification.class))).willAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId("notif-001");
                return n;
            });

            // Act
            Notification result = sut.createNotification(input);

            // Assert
            assertThat(result.getId()).isEqualTo("notif-001");
            assertThat(result.getType()).isEqualTo("GOAL_WARNING");
            then(notificationRepository).should().save(input);
        }

        @Test
        @DisplayName("Should throw exception when userId is blank")
        void shouldThrowException_WhenUserIdIsBlank() {
            Notification input = createNotification(null, "", "GOAL_WARNING",
                    "Title", "Message", false);

            assertThatThrownBy(() -> sut.createNotification(input))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("Should throw exception when type is invalid")
        void shouldThrowException_WhenTypeIsInvalid() {
            Notification input = createNotification(null, "user-123", "INVALID_TYPE",
                    "Title", "Message", false);

            assertThatThrownBy(() -> sut.createNotification(input))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("type");
        }

        @Test
        @DisplayName("Should throw exception when title is blank")
        void shouldThrowException_WhenTitleIsBlank() {
            Notification input = createNotification(null, "user-123", "GOAL_WARNING",
                    "", "Message", false);

            assertThatThrownBy(() -> sut.createNotification(input))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("title");
        }

        @Test
        @DisplayName("Should throw exception when message is blank")
        void shouldThrowException_WhenMessageIsBlank() {
            Notification input = createNotification(null, "user-123", "GOAL_WARNING",
                    "Title", "", false);

            assertThatThrownBy(() -> sut.createNotification(input))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("message");
        }
    }

    @Nested
    @DisplayName("getNotificationsByUser")
    class GetNotificationsByUserTests {

        @Test
        @DisplayName("Should return all notifications for a user")
        void shouldReturnNotifications_ForUser() {
            // Arrange
            String userId = "user-123";
            List<Notification> notifications = List.of(
                    createNotification("n1", userId, "GOAL_WARNING", "Alert", "Msg1", false),
                    createNotification("n2", userId, "BUDGET_TIP", "Tip", "Msg2", true)
            );
            given(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)).willReturn(notifications);

            // Act
            List<Notification> result = sut.getNotificationsByUser(userId);

            // Assert
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list when no notifications")
        void shouldReturnEmpty_WhenNone() {
            given(notificationRepository.findByUserIdOrderByCreatedAtDesc("no-notifs")).willReturn(List.of());

            List<Notification> result = sut.getNotificationsByUser("no-notifs");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUnreadNotifications")
    class GetUnreadNotificationsTests {

        @Test
        @DisplayName("Should return only unread notifications for a user")
        void shouldReturnOnlyUnread() {
            // Arrange
            String userId = "user-123";
            List<Notification> unread = List.of(
                    createNotification("n1", userId, "GOAL_WARNING", "Alert", "Msg", false)
            );
            given(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)).willReturn(unread);

            // Act
            List<Notification> result = sut.getUnreadNotifications(userId);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).isRead()).isFalse();
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should mark notification as read")
        void shouldMarkAsRead() {
            // Arrange
            String notifId = "notif-001";
            Notification n = createNotification(notifId, "user-123", "GOAL_WARNING",
                    "Alert", "Msg", false);
            given(notificationRepository.findById(notifId)).willReturn(Optional.of(n));
            given(notificationRepository.save(any(Notification.class))).willAnswer(inv -> inv.getArgument(0));

            // Act
            Notification result = sut.markAsRead(notifId);

            // Assert
            assertThat(result.isRead()).isTrue();
            then(notificationRepository).should().save(n);
        }

        @Test
        @DisplayName("Should throw exception when notification not found")
        void shouldThrowException_WhenNotFound() {
            given(notificationRepository.findById("nonexistent")).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.markAsRead("nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("Should mark all user notifications as read")
        void shouldMarkAllAsRead() {
            // Arrange
            String userId = "user-123";
            List<Notification> unread = List.of(
                    createNotification("n1", userId, "GOAL_WARNING", "Alert1", "Msg1", false),
                    createNotification("n2", userId, "BUDGET_TIP", "Tip", "Msg2", false)
            );
            given(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)).willReturn(unread);
            given(notificationRepository.saveAll(any())).willAnswer(inv -> inv.getArgument(0));

            // Act
            sut.markAllAsRead(userId);

            // Assert
            then(notificationRepository).should().saveAll(any());
        }
    }

    @Nested
    @DisplayName("deleteNotification")
    class DeleteNotificationTests {

        @Test
        @DisplayName("Should delete notification when it exists")
        void shouldDelete_WhenExists() {
            // Arrange
            String notifId = "notif-001";
            Notification n = createNotification(notifId, "user-123", "GOAL_WARNING",
                    "Alert", "Msg", true);
            given(notificationRepository.findById(notifId)).willReturn(Optional.of(n));

            // Act
            sut.deleteNotification(notifId);

            // Assert
            then(notificationRepository).should().delete(n);
        }

        @Test
        @DisplayName("Should throw exception when notification not found")
        void shouldThrowException_WhenNotFound() {
            given(notificationRepository.findById("nonexistent")).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.deleteNotification("nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Should return correct unread count")
        void shouldReturnCorrectCount() {
            // Arrange
            String userId = "user-123";
            given(notificationRepository.countByUserIdAndReadFalse(userId)).willReturn(5L);

            // Act
            long count = sut.getUnreadCount(userId);

            // Assert
            assertThat(count).isEqualTo(5L);
        }
    }
}
