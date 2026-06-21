package com.tracker.MoneyTracker.ai.service;

import com.tracker.MoneyTracker.ai.dto.HealthScoreResponse;
import com.tracker.MoneyTracker.analytics.AnalyticsService;
import com.tracker.MoneyTracker.analytics.dto.SpendingSummary;
import com.tracker.MoneyTracker.goal.GoalService;
import com.tracker.MoneyTracker.goal.GoalProgress;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HealthScoreService")
class HealthScoreServiceTest {

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private GoalService goalService;

    private HealthScoreService sut;

    @BeforeEach
    void setUp() {
        sut = new HealthScoreService(analyticsService, goalService);
    }

    @Test
    @DisplayName("Should return high score when user has good savings and goals")
    void shouldReturnHighScore_WhenUserHasGoodSavingsAndGoals() {
        // Arrange
        String userId = "user-123";
        // Income=100000, Expense=60000, Savings=40000 (40% savings rate)
        when(analyticsService.getSpendingSummary(userId))
                .thenReturn(new SpendingSummary(
                        BigDecimal.valueOf(100000), BigDecimal.valueOf(60000),
                        BigDecimal.valueOf(40000), "Food", 100));
        when(goalService.evaluateAllGoals(userId))
                .thenReturn(List.of(
                        new GoalProgress("g1", "Food", BigDecimal.valueOf(5000),
                                BigDecimal.valueOf(2000), BigDecimal.valueOf(3000), 40.0, "MONTHLY")
                ));

        // Act
        HealthScoreResponse response = sut.calculateScore(userId);

        // Assert
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.score()).isGreaterThanOrEqualTo(60);
        assertThat(response.savingsRate()).isEqualTo(40.0);
        assertThat(response.breakdown()).contains("Financial Health Score");
    }

    @Test
    @DisplayName("Should return low score when user has negative savings")
    void shouldReturnLowScore_WhenUserHasNegativeSavings() {
        // Arrange
        String userId = "user-456";
        // Income=50000, Expense=60000, Savings=-10000 (deficit)
        when(analyticsService.getSpendingSummary(userId))
                .thenReturn(new SpendingSummary(
                        BigDecimal.valueOf(50000), BigDecimal.valueOf(60000),
                        BigDecimal.valueOf(-10000), "Shopping", 80));
        when(goalService.evaluateAllGoals(userId)).thenReturn(List.of());

        // Act
        HealthScoreResponse response = sut.calculateScore(userId);

        // Assert
        assertThat(response.score()).isLessThan(40);
        assertThat(response.savingsRate()).isEqualTo(-20.0);
        assertThat(response.breakdown()).contains("Needs attention");
    }

    @Test
    @DisplayName("Should return high score when user has excellent finances")
    void shouldReturnHighScore_WhenUserHasExcellentFinances() {
        // Arrange
        String userId = "user-789";
        // Income=200000, Expense=100000, Savings=100000 (50% savings rate)
        // Scoring: savings=40 (50% rate), consistency=10 (default), goals=20 (20% = on track), budget=20 (positive) = 90
        when(analyticsService.getSpendingSummary(userId))
                .thenReturn(new SpendingSummary(
                        BigDecimal.valueOf(200000), BigDecimal.valueOf(100000),
                        BigDecimal.valueOf(100000), "Investment", 200));
        when(goalService.evaluateAllGoals(userId))
                .thenReturn(List.of(
                        new GoalProgress("g1", "Emergency", BigDecimal.valueOf(50000),
                                BigDecimal.valueOf(10000), BigDecimal.valueOf(40000), 20.0, "MONTHLY")
                ));

        // Act
        HealthScoreResponse response = sut.calculateScore(userId);

        // Assert
        assertThat(response.score()).isEqualTo(90);
        assertThat(response.breakdown()).contains("Excellent");
    }

    @Test
    @DisplayName("Should handle zero income gracefully")
    void shouldHandleZeroIncome_Gracefully() {
        // Arrange
        String userId = "user-zero";
        when(analyticsService.getSpendingSummary(userId))
                .thenReturn(new SpendingSummary(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "N/A", 0));
        when(goalService.evaluateAllGoals(userId)).thenReturn(List.of());

        // Act
        HealthScoreResponse response = sut.calculateScore(userId);

        // Assert
        assertThat(response.score()).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(100);
        assertThat(response.savingsRate()).isEqualTo(0.0);
    }
}
