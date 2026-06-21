package com.tracker.MoneyTracker.goal;

import com.tracker.MoneyTracker.exception.BadRequestException;
import com.tracker.MoneyTracker.exception.ResourceNotFoundException;
import com.tracker.MoneyTracker.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoalService")
class GoalServiceTest {

    @Mock
    private SpendingGoalRepository goalRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private GoalService sut;

    @BeforeEach
    void setUp() {
        sut = new GoalService(goalRepository, transactionRepository);
    }

    private SpendingGoal createGoal(String id, String userId, String category, BigDecimal target,
                                     String period, LocalDate startDate, LocalDate endDate, boolean active) {
        SpendingGoal goal = new SpendingGoal();
        goal.setId(id);
        goal.setUserId(userId);
        goal.setCategory(category);
        goal.setTargetAmount(target);
        goal.setPeriod(period);
        goal.setStartDate(startDate);
        goal.setEndDate(endDate);
        goal.setActive(active);
        return goal;
    }

    @Nested
    @DisplayName("createGoal")
    class CreateGoalTests {

        @Test
        @DisplayName("Should create and save a valid goal")
        void shouldCreateAndSave_WhenValidGoal() {
            // Arrange
            SpendingGoal goal = createGoal(null, "user-123", "FOOD",
                    new BigDecimal("5000.00"), "MONTHLY",
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), true);
            given(goalRepository.save(any(SpendingGoal.class))).willAnswer(invocation -> {
                SpendingGoal g = invocation.getArgument(0);
                g.setId("goal-001");
                return g;
            });

            // Act
            SpendingGoal result = sut.createGoal(goal);

            // Assert
            assertThat(result.getId()).isEqualTo("goal-001");
            then(goalRepository).should().save(goal);
        }

        @Test
        @DisplayName("Should throw exception when target amount is null")
        void shouldThrowException_WhenTargetAmountIsNull() {
            SpendingGoal goal = createGoal(null, "user-123", "FOOD",
                    null, "MONTHLY", LocalDate.now(), null, true);

            assertThatThrownBy(() -> sut.createGoal(goal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("targetAmount");
        }

        @Test
        @DisplayName("Should throw exception when target amount is zero")
        void shouldThrowException_WhenTargetAmountIsZero() {
            SpendingGoal goal = createGoal(null, "user-123", "FOOD",
                    BigDecimal.ZERO, "MONTHLY", LocalDate.now(), null, true);

            assertThatThrownBy(() -> sut.createGoal(goal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("targetAmount");
        }

        @Test
        @DisplayName("Should throw exception when category is blank")
        void shouldThrowException_WhenCategoryIsBlank() {
            SpendingGoal goal = createGoal(null, "user-123", "",
                    new BigDecimal("1000.00"), "MONTHLY", LocalDate.now(), null, true);

            assertThatThrownBy(() -> sut.createGoal(goal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("category");
        }

        @Test
        @DisplayName("Should throw exception when period is invalid")
        void shouldThrowException_WhenPeriodIsInvalid() {
            SpendingGoal goal = createGoal(null, "user-123", "FOOD",
                    new BigDecimal("1000.00"), "DAILY", LocalDate.now(), null, true);

            assertThatThrownBy(() -> sut.createGoal(goal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("period");
        }
    }

    @Nested
    @DisplayName("getGoalsByUser")
    class GetGoalsByUserTests {

        @Test
        @DisplayName("Should return all goals for a user")
        void shouldReturnGoals_ForUser() {
            // Arrange
            String userId = "user-123";
            List<SpendingGoal> goals = List.of(
                    createGoal("g1", userId, "FOOD", new BigDecimal("5000"), "MONTHLY",
                            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), true),
                    createGoal("g2", userId, "TRANSPORT", new BigDecimal("3000"), "MONTHLY",
                            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), true)
            );
            given(goalRepository.findByUserId(userId)).willReturn(goals);

            // Act
            List<SpendingGoal> result = sut.getGoalsByUser(userId);

            // Assert
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list when no goals")
        void shouldReturnEmpty_WhenNoGoals() {
            given(goalRepository.findByUserId("no-goals-user")).willReturn(List.of());

            List<SpendingGoal> result = sut.getGoalsByUser("no-goals-user");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getActiveGoalsByUser")
    class GetActiveGoalsByUserTests {

        @Test
        @DisplayName("Should return only active goals for a user")
        void shouldReturnOnlyActiveGoals() {
            // Arrange
            String userId = "user-123";
            List<SpendingGoal> activeGoals = List.of(
                    createGoal("g1", userId, "FOOD", new BigDecimal("5000"), "MONTHLY",
                            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), true)
            );
            given(goalRepository.findByUserIdAndActive(userId, true)).willReturn(activeGoals);

            // Act
            List<SpendingGoal> result = sut.getActiveGoalsByUser(userId);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("updateGoal")
    class UpdateGoalTests {

        @Test
        @DisplayName("Should update goal when it exists")
        void shouldUpdateGoal_WhenExists() {
            // Arrange
            String goalId = "goal-001";
            SpendingGoal existing = createGoal(goalId, "user-123", "FOOD",
                    new BigDecimal("5000"), "MONTHLY",
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), true);
            given(goalRepository.findById(goalId)).willReturn(Optional.of(existing));
            given(goalRepository.save(any(SpendingGoal.class))).willAnswer(inv -> inv.getArgument(0));

            SpendingGoal updated = createGoal(goalId, "user-123", "FOOD",
                    new BigDecimal("7000"), "MONTHLY",
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), true);

            // Act
            SpendingGoal result = sut.updateGoal(goalId, updated);

            // Assert
            assertThat(result.getTargetAmount()).isEqualByComparingTo(new BigDecimal("7000"));
            then(goalRepository).should().save(existing);
        }

        @Test
        @DisplayName("Should throw exception when goal not found")
        void shouldThrowException_WhenGoalNotFound() {
            given(goalRepository.findById("nonexistent")).willReturn(Optional.empty());

            SpendingGoal updated = createGoal("nonexistent", "user-123", "FOOD",
                    new BigDecimal("1000"), "MONTHLY", LocalDate.now(), null, true);

            assertThatThrownBy(() -> sut.updateGoal("nonexistent", updated))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("deleteGoal")
    class DeleteGoalTests {

        @Test
        @DisplayName("Should delete goal when it exists")
        void shouldDeleteGoal_WhenExists() {
            // Arrange
            String goalId = "goal-001";
            SpendingGoal goal = createGoal(goalId, "user-123", "FOOD",
                    new BigDecimal("5000"), "MONTHLY",
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), true);
            given(goalRepository.findById(goalId)).willReturn(Optional.of(goal));

            // Act
            sut.deleteGoal(goalId);

            // Assert
            then(goalRepository).should().delete(goal);
        }

        @Test
        @DisplayName("Should throw exception when goal not found")
        void shouldThrowException_WhenGoalNotFound() {
            given(goalRepository.findById("nonexistent")).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.deleteGoal("nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("getGoalProgress")
    class GetGoalProgressTests {

        @Test
        @DisplayName("Should calculate correct progress percentage")
        void shouldCalculateCorrectProgress() {
            // Arrange
            String goalId = "goal-001";
            String userId = "user-123";
            SpendingGoal goal = createGoal(goalId, userId, "FOOD",
                    new BigDecimal("1000"), "MONTHLY",
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), true);
            given(goalRepository.findById(goalId)).willReturn(Optional.of(goal));

            // Act
            GoalProgress result = sut.getGoalProgress(goalId);

            // Assert
            assertThat(result.goalId()).isEqualTo(goalId);
            assertThat(result.category()).isEqualTo("FOOD");
            assertThat(result.targetAmount()).isEqualByComparingTo(new BigDecimal("1000"));
            assertThat(result.period()).isEqualTo("MONTHLY");
        }

        @Test
        @DisplayName("Should throw exception when goal not found")
        void shouldThrowException_WhenGoalNotFound() {
            given(goalRepository.findById("nonexistent")).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.getGoalProgress("nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("evaluateAllGoals")
    class EvaluateAllGoalsTests {

        @Test
        @DisplayName("Should return goals needing attention when some are at risk or exceeded")
        void shouldReturnGoalsNeedingAttention_WhenSomeAtRisk() {
            // Arrange
            String userId = "user-123";
            SpendingGoal foodGoal = createGoal("g1", userId, "FOOD",
                    new BigDecimal("1000"), "MONTHLY",
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), true);
            SpendingGoal transportGoal = createGoal("g2", userId, "TRANSPORT",
                    new BigDecimal("5000"), "MONTHLY",
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), true);
            given(goalRepository.findByUserIdAndActive(userId, true)).willReturn(List.of(foodGoal, transportGoal));
            given(goalRepository.findById("g1")).willReturn(Optional.of(foodGoal));
            given(goalRepository.findById("g2")).willReturn(Optional.of(transportGoal));

            // FOOD: spent 900 out of 1000 = 90% → WARNING
            // TRANSPORT: spent 1000 out of 5000 = 20% → healthy
            com.tracker.MoneyTracker.transaction.Transaction foodTx = new com.tracker.MoneyTracker.transaction.Transaction();
            foodTx.setId("t1");
            foodTx.setUserId(userId);
            foodTx.setTransactionDate(LocalDate.of(2026, 6, 15));
            foodTx.setAmount(new BigDecimal("900"));
            foodTx.setType("DEBIT");
            foodTx.setCategory("FOOD");

            com.tracker.MoneyTracker.transaction.Transaction transportTx = new com.tracker.MoneyTracker.transaction.Transaction();
            transportTx.setId("t2");
            transportTx.setUserId(userId);
            transportTx.setTransactionDate(LocalDate.of(2026, 6, 10));
            transportTx.setAmount(new BigDecimal("1000"));
            transportTx.setType("DEBIT");
            transportTx.setCategory("TRANSPORT");

            given(transactionRepository.findByUserIdAndTransactionDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(List.of(foodTx, transportTx));

            // Act
            List<GoalProgress> result = sut.evaluateAllGoals(userId);

            // Assert — only FOOD goal (90%) should be returned, TRANSPORT (20%) should not
            assertThat(result).hasSize(1);
            assertThat(result.get(0).category()).isEqualTo("FOOD");
            assertThat(result.get(0).percentageUsed()).isGreaterThanOrEqualTo(80.0);
        }

        @Test
        @DisplayName("Should return empty when all goals are below 80% threshold")
        void shouldReturnEmpty_WhenAllGoalsHealthy() {
            // Arrange
            String userId = "user-123";
            SpendingGoal goal = createGoal("g1", userId, "FOOD",
                    new BigDecimal("1000"), "MONTHLY",
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), true);
            given(goalRepository.findByUserIdAndActive(userId, true)).willReturn(List.of(goal));
            given(goalRepository.findById("g1")).willReturn(Optional.of(goal));
            given(transactionRepository.findByUserIdAndTransactionDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(List.of());

            // Act
            List<GoalProgress> result = sut.evaluateAllGoals(userId);

            // Assert — no transactions means 0% spent, below 80% threshold
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty when no active goals exist")
        void shouldReturnEmpty_WhenNoActiveGoals() {
            // Arrange
            given(goalRepository.findByUserIdAndActive("user-123", true)).willReturn(List.of());

            // Act
            List<GoalProgress> result = sut.evaluateAllGoals("user-123");

            // Assert
            assertThat(result).isEmpty();
            then(goalRepository).should(never()).findById(any());
        }
    }
}
