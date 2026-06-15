package com.tracker.MoneyTracker.goal;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoalController")
class GoalControllerTest {

    @Mock
    private GoalService goalService;

    private GoalController sut;

    @BeforeEach
    void setUp() {
        sut = new GoalController(goalService);
    }

    private SpendingGoal createGoal(String id, String userId, String category, BigDecimal target,
                                     String period, LocalDate startDate, boolean active) {
        SpendingGoal goal = new SpendingGoal();
        goal.setId(id);
        goal.setUserId(userId);
        goal.setCategory(category);
        goal.setTargetAmount(target);
        goal.setPeriod(period);
        goal.setStartDate(startDate);
        goal.setEndDate(null);
        goal.setActive(active);
        return goal;
    }

    @Nested
    @DisplayName("createGoal")
    class CreateGoalTests {

        @Test
        @DisplayName("Should create goal and return created status")
        void shouldCreateGoal_AndReturnCreated() {
            SpendingGoal input = createGoal(null, "user-123", "FOOD",
                    new BigDecimal("5000"), "MONTHLY", LocalDate.of(2026, 6, 1), true);
            SpendingGoal saved = createGoal("goal-001", "user-123", "FOOD",
                    new BigDecimal("5000"), "MONTHLY", LocalDate.of(2026, 6, 1), true);
            given(goalService.createGoal(any(SpendingGoal.class))).willReturn(saved);

            ResponseEntity<SpendingGoal> result = sut.createGoal(input);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getId()).isEqualTo("goal-001");
        }
    }

    @Nested
    @DisplayName("getGoals")
    class GetGoalsTests {

        @Test
        @DisplayName("Should return goals for user")
        void shouldReturnGoals_ForUser() {
            List<SpendingGoal> goals = List.of(
                    createGoal("g1", "user-123", "FOOD", new BigDecimal("5000"), "MONTHLY", LocalDate.now(), true),
                    createGoal("g2", "user-123", "TRANSPORT", new BigDecimal("3000"), "MONTHLY", LocalDate.now(), true)
            );
            given(goalService.getGoalsByUser("user-123")).willReturn(goals);

            ResponseEntity<List<SpendingGoal>> result = sut.getGoals("user-123");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("Should throw BadRequestException when userId is blank")
        void shouldThrowBadRequest_WhenUserIdIsBlank() {
            assertThatThrownBy(() -> sut.getGoals(""))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("getActiveGoals")
    class GetActiveGoalsTests {

        @Test
        @DisplayName("Should return active goals for user")
        void shouldReturnActiveGoals_ForUser() {
            List<SpendingGoal> goals = List.of(
                    createGoal("g1", "user-123", "FOOD", new BigDecimal("5000"), "MONTHLY", LocalDate.now(), true)
            );
            given(goalService.getActiveGoalsByUser("user-123")).willReturn(goals);

            ResponseEntity<List<SpendingGoal>> result = sut.getActiveGoals("user-123");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw BadRequestException when userId is blank")
        void shouldThrowBadRequest_WhenUserIdIsBlank() {
            assertThatThrownBy(() -> sut.getActiveGoals(""))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("updateGoal")
    class UpdateGoalTests {

        @Test
        @DisplayName("Should update goal and return ok")
        void shouldUpdateGoal_AndReturnOk() {
            SpendingGoal updated = createGoal("goal-001", "user-123", "FOOD",
                    new BigDecimal("7000"), "MONTHLY", LocalDate.now(), true);
            given(goalService.updateGoal(eq("goal-001"), any(SpendingGoal.class))).willReturn(updated);

            ResponseEntity<SpendingGoal> result = sut.updateGoal("goal-001", updated);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getTargetAmount()).isEqualByComparingTo(new BigDecimal("7000"));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when goal does not exist")
        void shouldThrowNotFound_WhenGoalDoesNotExist() {
            SpendingGoal updated = createGoal("nonexistent", "user-123", "FOOD",
                    new BigDecimal("1000"), "MONTHLY", LocalDate.now(), true);
            given(goalService.updateGoal(eq("nonexistent"), any(SpendingGoal.class)))
                    .willThrow(new ResourceNotFoundException(ErrorCode.GOAL_NOT_FOUND, "nonexistent"));

            assertThatThrownBy(() -> sut.updateGoal("nonexistent", updated))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteGoal")
    class DeleteGoalTests {

        @Test
        @DisplayName("Should delete goal and return no content")
        void shouldDeleteGoal_AndReturnNoContent() {
            willDoNothing().given(goalService).deleteGoal("goal-001");

            ResponseEntity<Void> result = sut.deleteGoal("goal-001");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when goal does not exist")
        void shouldThrowNotFound_WhenGoalDoesNotExist() {
            willThrow(new ResourceNotFoundException(ErrorCode.GOAL_NOT_FOUND, "nonexistent"))
                    .given(goalService).deleteGoal("nonexistent");

            assertThatThrownBy(() -> sut.deleteGoal("nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getGoalProgress")
    class GetGoalProgressTests {

        @Test
        @DisplayName("Should return goal progress")
        void shouldReturnGoalProgress() {
            GoalProgress progress = new GoalProgress(
                    "goal-001", "FOOD", new BigDecimal("1000"),
                    new BigDecimal("500"), new BigDecimal("500"), 50.0, "MONTHLY"
            );
            given(goalService.getGoalProgress("goal-001")).willReturn(progress);

            ResponseEntity<GoalProgress> result = sut.getGoalProgress("goal-001");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().percentageUsed()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when goal does not exist")
        void shouldThrowNotFound_WhenGoalDoesNotExist() {
            given(goalService.getGoalProgress("nonexistent"))
                    .willThrow(new ResourceNotFoundException(ErrorCode.GOAL_NOT_FOUND, "nonexistent"));

            assertThatThrownBy(() -> sut.getGoalProgress("nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
