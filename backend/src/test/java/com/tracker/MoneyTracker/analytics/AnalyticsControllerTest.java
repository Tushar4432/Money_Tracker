package com.tracker.MoneyTracker.analytics;

import com.tracker.MoneyTracker.analytics.dto.*;
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
@DisplayName("AnalyticsController")
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    private AnalyticsController sut;

    @BeforeEach
    void setUp() {
        sut = new AnalyticsController(analyticsService);
    }

    @Nested
    @DisplayName("getSpendingSummary")
    class GetSpendingSummaryTests {

        @Test
        @DisplayName("Should return spending summary for valid userId")
        void shouldReturnSummary_ForValidUserId() {
            // Arrange
            SpendingSummary summary = new SpendingSummary(
                    new BigDecimal("5000.00"), new BigDecimal("300.00"),
                    new BigDecimal("4700.00"), "FOOD", 10
            );
            given(analyticsService.getSpendingSummary("user-123")).willReturn(summary);

            // Act
            ResponseEntity<SpendingSummary> result = sut.getSpendingSummary("user-123");

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().totalIncome()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(result.getBody().topCategory()).isEqualTo("FOOD");
        }

        @Test
        @DisplayName("Should return bad request when userId is blank")
        void shouldReturnBadRequest_WhenUserIdIsBlank() {
            ResponseEntity<SpendingSummary> result = sut.getSpendingSummary("");
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return bad request when userId is null")
        void shouldReturnBadRequest_WhenUserIdIsNull() {
            ResponseEntity<SpendingSummary> result = sut.getSpendingSummary(null);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("getCategoryBreakdown")
    class GetCategoryBreakdownTests {

        @Test
        @DisplayName("Should return category breakdown for valid request")
        void shouldReturnBreakdown_ForValidRequest() {
            // Arrange
            List<CategoryBreakdown> breakdown = List.of(
                    new CategoryBreakdown("FOOD", new BigDecimal("800.00"), 80.0),
                    new CategoryBreakdown("TRANSPORT", new BigDecimal("200.00"), 20.0)
            );
            LocalDate start = LocalDate.of(2026, 3, 1);
            LocalDate end = LocalDate.of(2026, 3, 31);
            given(analyticsService.getCategoryBreakdown("user-123", start, end)).willReturn(breakdown);

            // Act
            ResponseEntity<List<CategoryBreakdown>> result = sut.getCategoryBreakdown("user-123", start, end);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("Should return bad request when userId is blank")
        void shouldReturnBadRequest_WhenUserIdIsBlank() {
            ResponseEntity<List<CategoryBreakdown>> result = sut.getCategoryBreakdown("", null, null);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should work without date range parameters")
        void shouldWork_WithoutDateRange() {
            // Arrange
            given(analyticsService.getCategoryBreakdown("user-123", null, null)).willReturn(List.of());

            // Act
            ResponseEntity<List<CategoryBreakdown>> result = sut.getCategoryBreakdown("user-123", null, null);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getMonthlyTrends")
    class GetMonthlyTrendsTests {

        @Test
        @DisplayName("Should return monthly trends with default 6 months")
        void shouldReturnTrends_DefaultMonths() {
            // Arrange
            List<MonthlyTrend> trends = List.of(
                    new MonthlyTrend("2026-01", new BigDecimal("5000"), new BigDecimal("300"), new BigDecimal("4700")),
                    new MonthlyTrend("2026-02", new BigDecimal("5000"), new BigDecimal("400"), new BigDecimal("4600"))
            );
            given(analyticsService.getMonthlyTrends("user-123", 6)).willReturn(trends);

            // Act
            ResponseEntity<List<MonthlyTrend>> result = sut.getMonthlyTrends("user-123", 6);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("Should return bad request when userId is blank")
        void shouldReturnBadRequest_WhenUserIdIsBlank() {
            ResponseEntity<List<MonthlyTrend>> result = sut.getMonthlyTrends("", 6);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("getBudgetStatus")
    class GetBudgetStatusTests {

        @Test
        @DisplayName("Should return budget status for valid userId")
        void shouldReturnBudgetStatus_ForValidUserId() {
            // Arrange
            List<BudgetStatus> budget = List.of(
                    new BudgetStatus("FOOD", new BigDecimal("1000"), new BigDecimal("500"),
                            new BigDecimal("500"), 50.0, "MONTHLY")
            );
            given(analyticsService.getBudgetStatus("user-123")).willReturn(budget);

            // Act
            ResponseEntity<List<BudgetStatus>> result = sut.getBudgetStatus("user-123");

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody()).hasSize(1);
            assertThat(result.getBody().get(0).category()).isEqualTo("FOOD");
        }

        @Test
        @DisplayName("Should return bad request when userId is blank")
        void shouldReturnBadRequest_WhenUserIdIsBlank() {
            ResponseEntity<List<BudgetStatus>> result = sut.getBudgetStatus("");
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
