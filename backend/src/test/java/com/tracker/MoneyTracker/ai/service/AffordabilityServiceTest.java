package com.tracker.MoneyTracker.ai.service;

import com.tracker.MoneyTracker.ai.client.OllamaClient;
import com.tracker.MoneyTracker.ai.dto.AffordabilityResponse;
import com.tracker.MoneyTracker.analytics.AnalyticsService;
import com.tracker.MoneyTracker.analytics.dto.SpendingSummary;
import com.tracker.MoneyTracker.goal.GoalService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AffordabilityService")
class AffordabilityServiceTest {

    @Mock
    private OllamaClient ollamaClient;

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private GoalService goalService;

    private AffordabilityService sut;

    @BeforeEach
    void setUp() {
        Executor directExecutor = Runnable::run;
        sut = new AffordabilityService(ollamaClient, analyticsService, goalService, directExecutor);
    }

    @Test
    @DisplayName("Should return affordable when cost is within savings")
    void shouldReturnAffordable_WhenCostWithinSavings() {
        // Arrange
        String userId = "user-123";
        // Income=50000, Expense=30000, Savings=20000
        when(analyticsService.getSpendingSummary(userId))
                .thenReturn(new SpendingSummary(
                        BigDecimal.valueOf(50000), BigDecimal.valueOf(30000),
                        BigDecimal.valueOf(20000), "Food", 50));
        when(goalService.evaluateAllGoals(userId)).thenReturn(List.of());
        when(ollamaClient.generate(anyString())).thenReturn("You can afford this.");

        // Act
        AffordabilityResponse response = sut.analyze(userId, "PS5", BigDecimal.valueOf(15000));

        // Assert
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.itemName()).isEqualTo("PS5");
        assertThat(response.cost()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        assertThat(response.affordable()).isTrue();
        assertThat(response.analysis()).isEqualTo("You can afford this.");
    }

    @Test
    @DisplayName("Should return not affordable when user has negative savings")
    void shouldReturnNotAffordable_WhenUserHasNegativeSavings() {
        // Arrange
        String userId = "user-456";
        // Income=50000, Expense=60000, Savings=-10000
        when(analyticsService.getSpendingSummary(userId))
                .thenReturn(new SpendingSummary(
                        BigDecimal.valueOf(50000), BigDecimal.valueOf(60000),
                        BigDecimal.valueOf(-10000), "Shopping", 80));
        when(goalService.evaluateAllGoals(userId)).thenReturn(List.of());
        when(ollamaClient.generate(anyString())).thenReturn("You cannot afford this right now.");

        // Act
        AffordabilityResponse response = sut.analyze(userId, "MacBook", BigDecimal.valueOf(50000));

        // Assert
        assertThat(response.affordable()).isFalse();
        assertThat(response.analysis()).isEqualTo("You cannot afford this right now.");
    }

    @Test
    @DisplayName("Should include financial data in the prompt")
    void shouldIncludeFinancialData_InPrompt() {
        // Arrange
        String userId = "user-789";
        when(analyticsService.getSpendingSummary(userId))
                .thenReturn(new SpendingSummary(
                        BigDecimal.valueOf(100000), BigDecimal.valueOf(70000),
                        BigDecimal.valueOf(30000), "Travel", 100));
        when(goalService.evaluateAllGoals(userId)).thenReturn(List.of());
        when(ollamaClient.generate(anyString())).thenReturn("Yes, you can afford it.");

        // Act
        sut.analyze(userId, "Vacation", BigDecimal.valueOf(20000));

        // Assert
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(ollamaClient).generate(captor.capture());

        String prompt = captor.getValue();
        assertThat(prompt).contains("Vacation");
        assertThat(prompt).contains("20000");
        assertThat(prompt).contains("100000");  // income
        assertThat(prompt).contains("financial coach");
    }
}
