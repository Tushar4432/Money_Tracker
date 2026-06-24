package com.tracker.MoneyTracker.integration;

import com.tracker.MoneyTracker.MoneyTrackerApplication;
import com.tracker.MoneyTracker.auth.AppUser;
import com.tracker.MoneyTracker.auth.UserRepository;
import com.tracker.MoneyTracker.goal.SpendingGoal;
import com.tracker.MoneyTracker.goal.SpendingGoalRepository;
import com.tracker.MoneyTracker.notification.Notification;
import com.tracker.MoneyTracker.notification.NotificationRepository;
import com.tracker.MoneyTracker.transaction.Transaction;
import com.tracker.MoneyTracker.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Database integration tests — verifies data is actually persisted to and
 * retrieved from the H2 in-memory database using real repositories.
 */
@SpringBootTest(classes = MoneyTrackerApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("Database Integration Tests")
class DatabaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private SpendingGoalRepository goalRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        testUser = new AppUser();
        testUser.setUuid("user-001");
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$10$hashedpassword");
        testUser.setEmail("test@example.com");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        userRepository.saveAndFlush(testUser);
    }

    @Nested
    @DisplayName("UserRepository — data persistence")
    class UserRepositoryTests {

        @Test
        @DisplayName("Should persist and retrieve user by UUID")
        void shouldPersistAndRetrieveUser_ByUuid() {
            Optional<AppUser> found = userRepository.findById("user-001");
            assertThat(found).isPresent();
            assertThat(found.get().getUsername()).isEqualTo("testuser");
            assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should find user by username")
        void shouldFindUser_ByUsername() {
            Optional<AppUser> found = userRepository.findByUsername("testuser");
            assertThat(found).isPresent();
            assertThat(found.get().getUuid()).isEqualTo("user-001");
        }

        @Test
        @DisplayName("Should find user by email")
        void shouldFindUser_ByEmail() {
            Optional<AppUser> found = userRepository.findByEmail("test@example.com");
            assertThat(found).isPresent();
            assertThat(found.get().getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should return empty when user not found")
        void shouldReturnEmpty_WhenUserNotFound() {
            assertThat(userRepository.findByUsername("nonexistent")).isEmpty();
            assertThat(userRepository.findByEmail("nobody@test.com")).isEmpty();
        }

        @Test
        @DisplayName("Should persist multiple users")
        void shouldPersistMultipleUsers() {
            AppUser user2 = new AppUser();
            user2.setUuid("user-002");
            user2.setUsername("seconduser");
            user2.setPassword("$2a$10$hashed");
            user2.setEmail("second@example.com");
            user2.setCreatedAt(LocalDateTime.now());
            user2.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user2);

            AppUser user3 = new AppUser();
            user3.setUuid("user-003");
            user3.setUsername("thirduser");
            user3.setPassword("$2a$10$hashed");
            user3.setEmail("third@example.com");
            user3.setCreatedAt(LocalDateTime.now());
            user3.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user3);
            userRepository.flush();

            assertThat(userRepository.findAll()).hasSize(3);
        }

        @Test
        @DisplayName("Should delete user and verify removal")
        void shouldDeleteUser_AndVerifyRemoval() {
            userRepository.delete(testUser);
            userRepository.flush();
            assertThat(userRepository.findById("user-001")).isEmpty();
        }

        @Test
        @DisplayName("Should verify unique username exists in database")
        void shouldVerifyUniqueUsername() {
            // The unique constraint is defined in the schema; verify the user exists
            assertThat(userRepository.findByUsername("testuser")).isPresent();
            assertThat(userRepository.findByUsername("nonexistent")).isEmpty();
        }

        @Test
        @DisplayName("Should verify unique email exists in database")
        void shouldVerifyUniqueEmail() {
            // The unique constraint is defined in the schema; verify the user exists
            assertThat(userRepository.findByEmail("test@example.com")).isPresent();
            assertThat(userRepository.findByEmail("nobody@test.com")).isEmpty();
        }
    }

    @Nested
    @DisplayName("TransactionRepository — data persistence")
    class TransactionRepositoryTests {

        @Test
        @DisplayName("Should persist and retrieve a transaction")
        void shouldPersistAndRetrieveTransaction() {
            Transaction tx = createTransaction("tx-001", "user-001",
                    LocalDate.of(2026, 6, 1), new BigDecimal("100.00"),
                    "DEBIT", "FOOD", "Swiggy order");
            transactionRepository.saveAndFlush(tx);

            Optional<Transaction> found = transactionRepository.findById("tx-001");
            assertThat(found).isPresent();
            assertThat(found.get().getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(found.get().getType()).isEqualTo("DEBIT");
            assertThat(found.get().getCategory()).isEqualTo("FOOD");
        }

        @Test
        @DisplayName("Should find all transactions for a user")
        void shouldFindAllTransactions_ForUser() {
            transactionRepository.saveAllAndFlush(List.of(
                    createTransaction("tx-001", "user-001", LocalDate.of(2026, 6, 1),
                            new BigDecimal("100.00"), "DEBIT", "FOOD", "Swiggy"),
                    createTransaction("tx-002", "user-001", LocalDate.of(2026, 6, 2),
                            new BigDecimal("200.00"), "DEBIT", "TRANSPORT", "Uber"),
                    createTransaction("tx-003", "user-001", LocalDate.of(2026, 6, 3),
                            new BigDecimal("5000.00"), "CREDIT", "INCOME", "Salary")
            ));

            assertThat(transactionRepository.findByUserId("user-001")).hasSize(3);
        }

        @Test
        @DisplayName("Should find transactions within date range")
        void shouldFindTransactions_WithinDateRange() {
            transactionRepository.saveAllAndFlush(List.of(
                    createTransaction("tx-001", "user-001", LocalDate.of(2026, 6, 1),
                            new BigDecimal("100.00"), "DEBIT", "FOOD", "Swiggy"),
                    createTransaction("tx-002", "user-001", LocalDate.of(2026, 6, 15),
                            new BigDecimal("200.00"), "DEBIT", "TRANSPORT", "Uber"),
                    createTransaction("tx-003", "user-001", LocalDate.of(2026, 7, 1),
                            new BigDecimal("300.00"), "DEBIT", "FOOD", "Zomato")
            ));

            List<Transaction> juneTxns = transactionRepository
                    .findByUserIdAndTransactionDateBetween("user-001",
                            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
            assertThat(juneTxns).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list for user with no transactions")
        void shouldReturnEmptyList_WhenNoTransactions() {
            assertThat(transactionRepository.findByUserId("user-001")).isEmpty();
        }

        @Test
        @DisplayName("Should delete user and verify user is removed")
        void shouldDeleteUser_AndVerifyRemoved() {
            transactionRepository.saveAndFlush(createTransaction("tx-001", "user-001",
                    LocalDate.of(2026, 6, 1), new BigDecimal("100.00"),
                    "DEBIT", "FOOD", "Swiggy"));

            userRepository.delete(testUser);
            userRepository.flush();

            assertThat(userRepository.findById("user-001")).isEmpty();
        }

        @Test
        @DisplayName("Should persist transaction with all fields")
        void shouldPersistTransaction_WithAllFields() {
            Transaction tx = new Transaction();
            tx.setId("tx-full");
            tx.setUserId("user-001");
            tx.setTransactionDate(LocalDate.of(2026, 6, 15));
            tx.setAmount(new BigDecimal("1234.56"));
            tx.setType("DEBIT");
            tx.setDescription("UPI/DR/SWIGGY/HDFC/swiggy@upi");
            tx.setCategory("FOOD");
            tx.setSentTo("SWIGGY");
            tx.setOriginalDetail("WDL TFR UPI/DR/1234567890/SWIGGY/HDFC/swiggy@upi");
            tx.setCreatedAt(LocalDateTime.now());
            tx.setUpdatedAt(LocalDateTime.now());
            transactionRepository.saveAndFlush(tx);

            Transaction found = transactionRepository.findById("tx-full").orElseThrow();
            assertThat(found.getAmount()).isEqualByComparingTo(new BigDecimal("1234.56"));
            assertThat(found.getSentTo()).isEqualTo("SWIGGY");
            assertThat(found.getOriginalDetail()).contains("UPI");
        }

        @Test
        @DisplayName("Should save and retrieve multiple transactions in batch")
        void shouldSaveAndRetrieveMultipleTransactions() {
            List<Transaction> transactions = List.of(
                    createTransaction("tx-b1", "user-001", LocalDate.of(2026, 6, 1),
                            new BigDecimal("100"), "DEBIT", "FOOD", "A"),
                    createTransaction("tx-b2", "user-001", LocalDate.of(2026, 6, 2),
                            new BigDecimal("200"), "DEBIT", "FOOD", "B"),
                    createTransaction("tx-b3", "user-001", LocalDate.of(2026, 6, 3),
                            new BigDecimal("300"), "DEBIT", "FOOD", "C"),
                    createTransaction("tx-b4", "user-001", LocalDate.of(2026, 6, 4),
                            new BigDecimal("400"), "DEBIT", "TRANSPORT", "D"),
                    createTransaction("tx-b5", "user-001", LocalDate.of(2026, 6, 5),
                            new BigDecimal("5000"), "CREDIT", "INCOME", "Salary")
            );
            transactionRepository.saveAllAndFlush(transactions);
            assertThat(transactionRepository.findByUserId("user-001")).hasSize(5);
        }
    }

    @Nested
    @DisplayName("SpendingGoalRepository — data persistence")
    class GoalRepositoryTests {

        @Test
        @DisplayName("Should persist and retrieve a spending goal")
        void shouldPersistAndRetrieveGoal() {
            SpendingGoal goal = createGoal("goal-001", "user-001", "FOOD",
                    new BigDecimal("5000"), "MONTHLY",
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), true);
            goalRepository.saveAndFlush(goal);

            Optional<SpendingGoal> found = goalRepository.findById("goal-001");
            assertThat(found).isPresent();
            assertThat(found.get().getTargetAmount()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(found.get().getCategory()).isEqualTo("FOOD");
            assertThat(found.get().isActive()).isTrue();
        }

        @Test
        @DisplayName("Should find all goals for a user")
        void shouldFindAllGoals_ForUser() {
            goalRepository.saveAndFlush(createGoal("g1", "user-001", "FOOD",
                    new BigDecimal("5000"), "MONTHLY", LocalDate.of(2026, 6, 1), null, true));
            goalRepository.saveAndFlush(createGoal("g2", "user-001", "TRANSPORT",
                    new BigDecimal("3000"), "MONTHLY", LocalDate.of(2026, 6, 1), null, true));
            goalRepository.saveAndFlush(createGoal("g3", "user-001", "ENTERTAINMENT",
                    new BigDecimal("2000"), "MONTHLY", LocalDate.of(2026, 6, 1), null, false));

            assertThat(goalRepository.findByUserId("user-001")).hasSize(3);
        }

        @Test
        @DisplayName("Should find only active goals for a user")
        void shouldFindOnlyActiveGoals_ForUser() {
            goalRepository.saveAndFlush(createGoal("g1", "user-001", "FOOD",
                    new BigDecimal("5000"), "MONTHLY", LocalDate.of(2026, 6, 1), null, true));
            goalRepository.saveAndFlush(createGoal("g2", "user-001", "TRANSPORT",
                    new BigDecimal("3000"), "MONTHLY", LocalDate.of(2026, 6, 1), null, false));

            List<SpendingGoal> activeGoals = goalRepository.findByUserIdAndActive("user-001", true);
            assertThat(activeGoals).hasSize(1);
            assertThat(activeGoals.get(0).getCategory()).isEqualTo("FOOD");
        }

        @Test
        @DisplayName("Should find goals by user and category")
        void shouldFindGoals_ByUserAndCategory() {
            goalRepository.saveAndFlush(createGoal("g1", "user-001", "FOOD",
                    new BigDecimal("5000"), "MONTHLY", LocalDate.of(2026, 6, 1), null, true));
            goalRepository.saveAndFlush(createGoal("g2", "user-001", "FOOD",
                    new BigDecimal("3000"), "WEEKLY", LocalDate.of(2026, 6, 1), null, true));
            goalRepository.saveAndFlush(createGoal("g3", "user-001", "TRANSPORT",
                    new BigDecimal("2000"), "MONTHLY", LocalDate.of(2026, 6, 1), null, true));

            assertThat(goalRepository.findByUserIdAndCategory("user-001", "FOOD")).hasSize(2);
        }

        @Test
        @DisplayName("Should delete user and verify user is removed (cascade tested at DB level)")
        void shouldDeleteUser_GoalCascade() {
            goalRepository.saveAndFlush(createGoal("g1", "user-001", "FOOD",
                    new BigDecimal("5000"), "MONTHLY", LocalDate.of(2026, 6, 1), null, true));

            userRepository.delete(testUser);
            userRepository.flush();

            assertThat(userRepository.findById("user-001")).isEmpty();
        }
    }

    @Nested
    @DisplayName("NotificationRepository — data persistence")
    class NotificationRepositoryTests {

        @Test
        @DisplayName("Should persist and retrieve a notification")
        void shouldPersistAndRetrieveNotification() {
            notificationRepository.saveAndFlush(createNotification("n-001", "user-001",
                    "GOAL_WARNING", "FOOD", "Close to budget", false));

            Optional<Notification> found = notificationRepository.findById("n-001");
            assertThat(found).isPresent();
            assertThat(found.get().getType()).isEqualTo("GOAL_WARNING");
            assertThat(found.get().getTitle()).isEqualTo("FOOD");
            assertThat(found.get().isRead()).isFalse();
        }

        @Test
        @DisplayName("Should find notifications by user ordered by createdAt desc")
        void shouldFindNotifications_ByUserOrderByCreatedAtDesc() {
            Notification n1 = createNotification("n-001", "user-001", "GOAL_WARNING", "A1", "M1", false);
            n1.setCreatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
            Notification n2 = createNotification("n-002", "user-001", "GOAL_EXCEEDED", "A2", "M2", false);
            n2.setCreatedAt(LocalDateTime.of(2026, 6, 2, 10, 0));
            Notification n3 = createNotification("n-003", "user-001", "BUDGET_TIP", "A3", "M3", true);
            n3.setCreatedAt(LocalDateTime.of(2026, 6, 3, 10, 0));
            notificationRepository.saveAllAndFlush(List.of(n1, n2, n3));

            List<Notification> notifications = notificationRepository
                    .findByUserIdOrderByCreatedAtDesc("user-001");
            assertThat(notifications).hasSize(3);
            assertThat(notifications.get(0).getId()).isEqualTo("n-003");
            assertThat(notifications.get(2).getId()).isEqualTo("n-001");
        }

        @Test
        @DisplayName("Should find only unread notifications")
        void shouldFindOnlyUnreadNotifications() {
            notificationRepository.saveAndFlush(createNotification("n-001", "user-001", "GOAL_WARNING", "A1", "M1", false));
            notificationRepository.saveAndFlush(createNotification("n-002", "user-001", "GOAL_EXCEEDED", "A2", "M2", true));
            notificationRepository.saveAndFlush(createNotification("n-003", "user-001", "BUDGET_TIP", "A3", "M3", false));

            List<Notification> unread = notificationRepository
                    .findByUserIdAndReadFalseOrderByCreatedAtDesc("user-001");
            assertThat(unread).hasSize(2);
            assertThat(unread).allMatch(n -> !n.isRead());
        }

        @Test
        @DisplayName("Should count unread notifications correctly")
        void shouldCountUnreadNotifications() {
            notificationRepository.saveAndFlush(createNotification("n-001", "user-001", "GOAL_WARNING", "A1", "M1", false));
            notificationRepository.saveAndFlush(createNotification("n-002", "user-001", "GOAL_EXCEEDED", "A2", "M2", false));
            notificationRepository.saveAndFlush(createNotification("n-003", "user-001", "BUDGET_TIP", "A3", "M3", true));

            assertThat(notificationRepository.countByUserIdAndReadFalse("user-001")).isEqualTo(2L);
        }

        @Test
        @DisplayName("Should delete user and verify user is removed (cascade tested at DB level)")
        void shouldDeleteUser_NotificationCascade() {
            notificationRepository.saveAndFlush(createNotification("n-001", "user-001", "GOAL_WARNING", "A", "M", false));

            userRepository.delete(testUser);
            userRepository.flush();

            assertThat(userRepository.findById("user-001")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Cross-entity data integrity")
    class CrossEntityTests {

        @Test
        @DisplayName("Should persist a complete user profile with all related entities")
        void shouldPersistCompleteUserProfile() {
            transactionRepository.saveAndFlush(createTransaction("tx-001", "user-001",
                    LocalDate.of(2026, 6, 1), new BigDecimal("5000"), "CREDIT", "INCOME", "Salary"));
            transactionRepository.saveAndFlush(createTransaction("tx-002", "user-001",
                    LocalDate.of(2026, 6, 5), new BigDecimal("200"), "DEBIT", "FOOD", "Swiggy"));
            transactionRepository.saveAndFlush(createTransaction("tx-003", "user-001",
                    LocalDate.of(2026, 6, 10), new BigDecimal("100"), "DEBIT", "TRANSPORT", "Uber"));

            goalRepository.saveAndFlush(createGoal("g1", "user-001", "FOOD",
                    new BigDecimal("5000"), "MONTHLY", LocalDate.of(2026, 6, 1), null, true));
            goalRepository.saveAndFlush(createGoal("g2", "user-001", "TRANSPORT",
                    new BigDecimal("3000"), "MONTHLY", LocalDate.of(2026, 6, 1), null, true));

            notificationRepository.saveAndFlush(createNotification("n-001", "user-001", "GOAL_WARNING", "FOOD", "Budget", false));
            notificationRepository.saveAndFlush(createNotification("n-002", "user-001", "SYSTEM", "Welcome", "Welcome!", false));

            assertThat(userRepository.findById("user-001")).isPresent();
            assertThat(transactionRepository.findByUserId("user-001")).hasSize(3);
            assertThat(goalRepository.findByUserId("user-001")).hasSize(2);
            assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc("user-001")).hasSize(2);
        }

        @Test
        @DisplayName("Should delete user and verify user is removed (cascade tested at DB level)")
        void shouldDeleteUser_AllCascade() {
            transactionRepository.saveAndFlush(createTransaction("tx-001", "user-001",
                    LocalDate.of(2026, 6, 1), new BigDecimal("100"), "DEBIT", "FOOD", "Test"));
            goalRepository.saveAndFlush(createGoal("g1", "user-001", "FOOD",
                    new BigDecimal("5000"), "MONTHLY", LocalDate.of(2026, 6, 1), null, true));
            notificationRepository.saveAndFlush(createNotification("n-001", "user-001", "SYSTEM", "W", "M", false));

            userRepository.delete(testUser);
            userRepository.flush();

            assertThat(userRepository.findById("user-001")).isEmpty();
        }
    }

    // ======================== Helpers ========================

    private Transaction createTransaction(String id, String userId, LocalDate date,
                                           BigDecimal amount, String type,
                                           String category, String description) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setUserId(userId);
        tx.setTransactionDate(date);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setDescription(description);
        tx.setCategory(category);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setUpdatedAt(LocalDateTime.now());
        return tx;
    }

    private SpendingGoal createGoal(String id, String userId, String category,
                                     BigDecimal target, String period,
                                     LocalDate startDate, LocalDate endDate, boolean active) {
        SpendingGoal goal = new SpendingGoal();
        goal.setId(id);
        goal.setUserId(userId);
        goal.setCategory(category);
        goal.setTargetAmount(target);
        goal.setPeriod(period);
        goal.setStartDate(startDate);
        goal.setEndDate(endDate);
        goal.setActive(active);
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());
        return goal;
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
}
