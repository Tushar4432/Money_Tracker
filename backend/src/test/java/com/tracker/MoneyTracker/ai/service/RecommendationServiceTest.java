package com.tracker.MoneyTracker.ai.service;

import com.tracker.MoneyTracker.ai.client.AiLlmClient;
import com.tracker.MoneyTracker.ai.dto.RecommendationResponse;
import com.tracker.MoneyTracker.analytics.AnalyticsService;
import com.tracker.MoneyTracker.analytics.dto.CategoryBreakdown;
import com.tracker.MoneyTracker.analytics.dto.MonthlyTrend;
import com.tracker.MoneyTracker.analytics.dto.SpendingSummary;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService")
class RecommendationServiceTest {

    @Mock
    private AiLlmClient ollamaClient;

    @Mock
    private AnalyticsService analyticsService;

    private RecommendationService sut;

    @BeforeEach
    void setUp() {
        Executor directExecutor = Runnable::run;
        sut = new RecommendationService(ollamaClient, analyticsService, directExecutor);
    }

    @Test
    @DisplayName("Should return recommendations when AI generates valid response")
    void shouldReturnRecommendations_WhenAiGeneratesValidResponse() {
        // Arrange
        String userId = "user-123";
        when(analyticsService.getSpendingSummary(userId))
                .thenReturn(new SpendingSummary(
                        BigDecimal.valueOf(50000), BigDecimal.valueOf(40000),
                        BigDecimal.valueOf(10000), "Food", 50));
        when(analyticsService.getCategoryBreakdown(eq(userId), any(), any()))
                .thenReturn(List.of(
                        new CategoryBreakdown("Food", BigDecimal.valueOf(15000), 37.5),
                        new CategoryBreakdown("Shopping", BigDecimal.valueOf(10000), 25.0)
                ));
        when(analyticsService.getMonthlyTrends(eq(userId), anyInt()))
                .thenReturn(List.of());

        when(ollamaClient.generate(anyString())).thenReturn(
                "1. Reduce dining out by 50% to save ₹2000/month\n" +
                "2. Cancel unused subscriptions\n" +
                "3. Set up automatic savings transfer"
        );

        // Act
        RecommendationResponse response = sut.generateRecommendations(userId);

        // Assert
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.recommendations()).hasSize(3);
        assertThat(response.recommendations().get(0)).contains("Reduce dining out");
        assertThat(response.recommendations().get(1)).contains("Cancel unused subscriptions");
        assertThat(response.recommendations().get(2)).contains("automatic savings");
    }

    @Test
    @DisplayName("Should return fallback when AI returns empty response")
    void shouldReturnFallback_WhenAiReturnsEmptyResponse() {
        // Arrange
        String userId = "user-123";
        when(analyticsService.getSpendingSummary(userId))
                .thenReturn(new SpendingSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "N/A", 0));
        when(analyticsService.getCategoryBreakdown(eq(userId), any(), any())).thenReturn(List.of());
        when(analyticsService.getMonthlyTrends(eq(userId), anyInt())).thenReturn(List.of());
        when(ollamaClient.generate(anyString())).thenReturn("");

        // Act
        RecommendationResponse response = sut.generateRecommendations(userId);

        // Assert
        assertThat(response.recommendations()).hasSize(1);
        assertThat(response.recommendations().get(0)).contains("No recommendations available");
    }

    @Test
    @DisplayName("Should include spending data in the prompt")
    void shouldIncludeSpendingData_InPrompt() {
        // Arrange
        String userId = "user-123";
        when(analyticsService.getSpendingSummary(userId))
                .thenReturn(new SpendingSummary(
                        BigDecimal.valueOf(80000), BigDecimal.valueOf(60000),
                        BigDecimal.valueOf(20000), "Travel", 75));
        when(analyticsService.getCategoryBreakdown(eq(userId), any(), any())).thenReturn(List.of());
        when(analyticsService.getMonthlyTrends(eq(userId), anyInt())).thenReturn(List.of());
        when(ollamaClient.generate(anyString())).thenReturn("Save more.");

        // Act
        sut.generateRecommendations(userId);

        // Assert
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(ollamaClient).generate(captor.capture());

        String prompt = captor.getValue();
        assertThat(prompt).contains("80000");  // income
        assertThat(prompt).contains("Travel"); // top category
        assertThat(prompt).contains("financial coach");
    }
}
