package com.tracker.MoneyTracker.analytics;

import com.tracker.MoneyTracker.transaction.Transaction;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService")
class AnalyticsServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private AnalyticsService sut;

    @BeforeEach
    void setUp() {
        sut = new AnalyticsService(transactionRepository);
    }

    private Transaction createTransaction(String id, String userId, LocalDate date, BigDecimal amount,
                                           String type, String category, String description) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setUserId(userId);
        tx.setTransactionDate(date);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setCategory(category);
        tx.setDescription(description);
        return tx;
    }

    @Nested
    @DisplayName("getSpendingSummary")
    class GetSpendingSummaryTests {

        @Test
        @DisplayName("Should return correct income and expense totals")
        void shouldReturnCorrectTotals() {
            // Arrange
            String userId = "user-123";
            LocalDate date = LocalDate.of(2026, 3, 15);
            List<Transaction> transactions = List.of(
                    createTransaction("t1", userId, date, new BigDecimal("5000.00"), "CREDIT", "INCOME", "Salary"),
                    createTransaction("t2", userId, date, new BigDecimal("100.00"), "DEBIT", "FOOD", "Swiggy"),
                    createTransaction("t3", userId, date, new BigDecimal("200.00"), "DEBIT", "TRANSPORT", "Petrol")
            );
            given(transactionRepository.findByUserId(userId)).willReturn(transactions);

            // Act
            var summary = sut.getSpendingSummary(userId);

            // Assert
            assertThat(summary.totalIncome()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(summary.totalExpense()).isEqualByComparingTo(new BigDecimal("300.00"));
            assertThat(summary.netSavings()).isEqualByComparingTo(new BigDecimal("4700.00"));
            assertThat(summary.transactionCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should identify top spending category")
        void shouldIdentifyTopCategory() {
            // Arrange
            String userId = "user-123";
            LocalDate date = LocalDate.of(2026, 3, 15);
            List<Transaction> transactions = List.of(
                    createTransaction("t1", userId, date, new BigDecimal("500.00"), "DEBIT", "FOOD", "Swiggy"),
                    createTransaction("t2", userId, date, new BigDecimal("1000.00"), "DEBIT", "RENT", "Rent"),
                    createTransaction("t3", userId, date, new BigDecimal("200.00"), "DEBIT", "FOOD", "Zomato")
            );
            given(transactionRepository.findByUserId(userId)).willReturn(transactions);

            // Act
            var summary = sut.getSpendingSummary(userId);

            // Assert
            assertThat(summary.topCategory()).isEqualTo("RENT");
        }

        @Test
        @DisplayName("Should return N/A for top category when no debit transactions")
        void shouldReturnNA_WhenNoDebits() {
            // Arrange
            String userId = "user-123";
            List<Transaction> transactions = List.of(
                    createTransaction("t1", userId, LocalDate.now(), new BigDecimal("5000.00"), "CREDIT", "INCOME", "Salary")
            );
            given(transactionRepository.findByUserId(userId)).willReturn(transactions);

            // Act
            var summary = sut.getSpendingSummary(userId);

            // Assert
            assertThat(summary.topCategory()).isEqualTo("N/A");
        }

        @Test
        @DisplayName("Should return zeros when no transactions")
        void shouldReturnZeros_WhenNoTransactions() {
            // Arrange
            given(transactionRepository.findByUserId("empty-user")).willReturn(List.of());

            // Act
            var summary = sut.getSpendingSummary("empty-user");

            // Assert
            assertThat(summary.totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(summary.totalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(summary.netSavings()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(summary.topCategory()).isEqualTo("N/A");
            assertThat(summary.transactionCount()).isZero();
        }

        @Test
        @DisplayName("Should return null-safe result when repository is null")
        void shouldReturnNullSafe_WhenRepositoryIsNull() {
            // Arrange
            AnalyticsService nullService = new AnalyticsService();

            // Act
            var summary = nullService.getSpendingSummary("user-123");

            // Assert
            assertThat(summary.totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(summary.totalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("getCategoryBreakdown")
    class GetCategoryBreakdownTests {

        @Test
        @DisplayName("Should return category breakdown with percentages")
        void shouldReturnCategoryBreakdown() {
            // Arrange
            String userId = "user-123";
            LocalDate start = LocalDate.of(2026, 3, 1);
            LocalDate end = LocalDate.of(2026, 3, 31);
            List<Transaction> transactions = List.of(
                    createTransaction("t1", userId, start, new BigDecimal("300.00"), "DEBIT", "FOOD", "Swiggy"),
                    createTransaction("t2", userId, start, new BigDecimal("200.00"), "DEBIT", "TRANSPORT", "Petrol"),
                    createTransaction("t3", userId, start, new BigDecimal("500.00"), "DEBIT", "FOOD", "Zomato")
            );
            given(transactionRepository.findByUserIdAndTransactionDateBetween(userId, start, end)).willReturn(transactions);

            // Act
            var breakdown = sut.getCategoryBreakdown(userId, start, end);

            // Assert
            assertThat(breakdown).hasSize(2);
            // FOOD should be first (800 total, 80%)
            assertThat(breakdown.get(0).category()).isEqualTo("FOOD");
            assertThat(breakdown.get(0).amount()).isEqualByComparingTo(new BigDecimal("800.00"));
            assertThat(breakdown.get(0).percentage()).isEqualTo(80.0);
            // TRANSPORT should be second (200 total, 20%)
            assertThat(breakdown.get(1).category()).isEqualTo("TRANSPORT");
            assertThat(breakdown.get(1).amount()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(breakdown.get(1).percentage()).isEqualTo(20.0);
        }

        @Test
        @DisplayName("Should exclude credit transactions from breakdown")
        void shouldExcludeCredits() {
            // Arrange
            String userId = "user-123";
            LocalDate start = LocalDate.of(2026, 3, 1);
            LocalDate end = LocalDate.of(2026, 3, 31);
            List<Transaction> transactions = List.of(
                    createTransaction("t1", userId, start, new BigDecimal("5000.00"), "CREDIT", "INCOME", "Salary"),
                    createTransaction("t2", userId, start, new BigDecimal("100.00"), "DEBIT", "FOOD", "Swiggy")
            );
            given(transactionRepository.findByUserIdAndTransactionDateBetween(userId, start, end)).willReturn(transactions);

            // Act
            var breakdown = sut.getCategoryBreakdown(userId, start, end);

            // Assert
            assertThat(breakdown).hasSize(1);
            assertThat(breakdown.get(0).category()).isEqualTo("FOOD");
            assertThat(breakdown.get(0).percentage()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should return empty list when no expense transactions")
        void shouldReturnEmpty_WhenNoExpenses() {
            // Arrange
            String userId = "user-123";
            given(transactionRepository.findByUserId(userId)).willReturn(List.of());

            // Act
            var breakdown = sut.getCategoryBreakdown(userId, null, null);

            // Assert
            assertThat(breakdown).isEmpty();
        }

        @Test
        @DisplayName("Should sort categories by amount descending")
        void shouldSortByAmountDescending() {
            // Arrange
            String userId = "user-123";
            LocalDate start = LocalDate.of(2026, 3, 1);
            LocalDate end = LocalDate.of(2026, 3, 31);
            List<Transaction> transactions = List.of(
                    createTransaction("t1", userId, start, new BigDecimal("50.00"), "DEBIT", "FOOD", "Snack"),
                    createTransaction("t2", userId, start, new BigDecimal("500.00"), "DEBIT", "RENT", "Rent"),
                    createTransaction("t3", userId, start, new BigDecimal("100.00"), "DEBIT", "TRANSPORT", "Fuel")
            );
            given(transactionRepository.findByUserIdAndTransactionDateBetween(userId, start, end)).willReturn(transactions);

            // Act
            var breakdown = sut.getCategoryBreakdown(userId, start, end);

            // Assert
            assertThat(breakdown).extracting(b -> b.category()).containsExactly("RENT", "TRANSPORT", "FOOD");
        }
    }

    @Nested
    @DisplayName("getMonthlyTrends")
    class GetMonthlyTrendsTests {

        @Test
        @DisplayName("Should return monthly trends for specified number of months")
        void shouldReturnMonthlyTrends() {
            // Arrange
            String userId = "user-123";
            LocalDate now = LocalDate.now();
            List<Transaction> transactions = List.of(
                    createTransaction("t1", userId, now.withDayOfMonth(1), new BigDecimal("5000.00"), "CREDIT", "INCOME", "Salary"),
                    createTransaction("t2", userId, now.withDayOfMonth(5), new BigDecimal("300.00"), "DEBIT", "FOOD", "Swiggy")
            );
            given(transactionRepository.findByUserId(userId)).willReturn(transactions);

            // Act
            var trends = sut.getMonthlyTrends(userId, 3);

            // Assert
            assertThat(trends).hasSize(3);
        }

        @Test
        @DisplayName("Should calculate net as income minus expense")
        void shouldCalculateNetCorrectly() {
            // Arrange
            String userId = "user-123";
            LocalDate now = LocalDate.now();
            String monthKey = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            List<Transaction> transactions = List.of(
                    createTransaction("t1", userId, now.withDayOfMonth(1), new BigDecimal("1000.00"), "CREDIT", "INCOME", "Salary"),
                    createTransaction("t2", userId, now.withDayOfMonth(5), new BigDecimal("200.00"), "DEBIT", "FOOD", "Swiggy")
            );
            given(transactionRepository.findByUserId(userId)).willReturn(transactions);

            // Act
            var trends = sut.getMonthlyTrends(userId, 1);

            // Assert
            assertThat(trends).hasSize(1);
            assertThat(trends.get(0).month()).isEqualTo(monthKey);
            assertThat(trends.get(0).income()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(trends.get(0).expense()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(trends.get(0).net()).isEqualByComparingTo(new BigDecimal("800.00"));
        }

        @Test
        @DisplayName("Should return zero trends for months with no transactions")
        void shouldReturnZeroTrends_WhenNoTransactions() {
            // Arrange
            String userId = "user-123";
            given(transactionRepository.findByUserId(userId)).willReturn(List.of());

            // Act
            var trends = sut.getMonthlyTrends(userId, 3);

            // Assert
            assertThat(trends).hasSize(3);
            assertThat(trends).allMatch(t -> t.income().compareTo(BigDecimal.ZERO) == 0);
            assertThat(trends).allMatch(t -> t.expense().compareTo(BigDecimal.ZERO) == 0);
        }

        @Test
        @DisplayName("Should default to 6 months when months parameter is zero or negative")
        void shouldDefaultToSixMonths() {
            // Arrange
            String userId = "user-123";
            given(transactionRepository.findByUserId(userId)).willReturn(List.of());

            // Act
            var trends = sut.getMonthlyTrends(userId, 0);

            // Assert
            assertThat(trends).hasSize(6);
        }
    }

    @Nested
    @DisplayName("getBudgetStatus")
    class GetBudgetStatusTests {

        @Test
        @DisplayName("Should return current month spending per category")
        void shouldReturnCurrentMonthSpending() {
            // Arrange
            String userId = "user-123";
            LocalDate now = LocalDate.now();
            List<Transaction> transactions = List.of(
                    createTransaction("t1", userId, now, new BigDecimal("300.00"), "DEBIT", "FOOD", "Swiggy"),
                    createTransaction("t2", userId, now, new BigDecimal("300.00"), "DEBIT", "FOOD", "Zomato"),
                    createTransaction("t3", userId, now, new BigDecimal("500.00"), "DEBIT", "TRANSPORT", "Fuel")
            );
            given(transactionRepository.findByUserIdAndTransactionDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(transactions);

            // Act
            var budget = sut.getBudgetStatus(userId);

            // Assert
            assertThat(budget).hasSize(2);
            // FOOD total = 600, TRANSPORT total = 500
            assertThat(budget).extracting(b -> b.category()).containsExactlyInAnyOrder("FOOD", "TRANSPORT");
            var foodBudget = budget.stream().filter(b -> "FOOD".equals(b.category())).findFirst().orElseThrow();
            assertThat(foodBudget.spentAmount()).isEqualByComparingTo(new BigDecimal("600.00"));
        }

        @Test
        @DisplayName("Should return empty list when no transactions")
        void shouldReturnEmpty_WhenNoTransactions() {
            // Arrange
            String userId = "user-123";
            LocalDate start = LocalDate.now().withDayOfMonth(1);
            LocalDate end = LocalDate.now();
            given(transactionRepository.findByUserIdAndTransactionDateBetween(eq(userId), eq(start), eq(end)))
                    .willReturn(List.of());

            // Act
            var budget = sut.getBudgetStatus(userId);

            // Assert
            assertThat(budget).isEmpty();
        }
    }

    @Nested
    @DisplayName("getMonthlyTrends with period grouping")
    class GetMonthlyTrendsWithPeriodTests {

        @Test
        @DisplayName("Should return quarterly trends when period is QUARTERLY")
        void shouldReturnQuarterlyTrends_WhenPeriodIsQuarterly() {
            // Arrange
            String userId = "user-123";
            LocalDate now = LocalDate.now();
            List<Transaction> transactions = List.of(
                    createTransaction("t1", userId, now, new BigDecimal("300.00"), "DEBIT", "FOOD", "Swiggy")
            );
            given(transactionRepository.findByUserId(userId)).willReturn(transactions);

            // Act
            var trends = sut.getMonthlyTrends(userId, 4, "QUARTERLY");

            // Assert
            assertThat(trends).hasSize(4);
            assertThat(trends).allMatch(t -> t.month().contains("Q"));
        }

        @Test
        @DisplayName("Should return half-yearly trends when period is HALF_YEARLY")
        void shouldReturnHalfYearlyTrends_WhenPeriodIsHalfYearly() {
            // Arrange
            String userId = "user-123";
            LocalDate now = LocalDate.now();
            List<Transaction> transactions = List.of(
                    createTransaction("t1", userId, now, new BigDecimal("500.00"), "DEBIT", "RENT", "Rent")
            );
            given(transactionRepository.findByUserId(userId)).willReturn(transactions);

            // Act
            var trends = sut.getMonthlyTrends(userId, 4, "HALF_YEARLY");

            // Assert
            assertThat(trends).hasSize(4);
            assertThat(trends).allMatch(t -> t.month().contains("H"));
        }

        @Test
        @DisplayName("Should return yearly trends when period is YEARLY")
        void shouldReturnYearlyTrends_WhenPeriodIsYearly() {
            // Arrange
            String userId = "user-123";
            LocalDate now = LocalDate.now();
            List<Transaction> transactions = List.of(
                    createTransaction("t1", userId, now, new BigDecimal("1000.00"), "DEBIT", "RENT", "Rent")
            );
            given(transactionRepository.findByUserId(userId)).willReturn(transactions);

            // Act
            var trends = sut.getMonthlyTrends(userId, 3, "YEARLY");

            // Assert
            assertThat(trends).hasSize(3);
            assertThat(trends).allMatch(t -> t.month().matches("\\d{4}"));
        }

        @Test
        @DisplayName("Should default to MONTHLY when period is null")
        void shouldDefaultToMonthly_WhenPeriodIsNull() {
            // Arrange
            String userId = "user-123";
            given(transactionRepository.findByUserId(userId)).willReturn(List.of());

            // Act
            var trends = sut.getMonthlyTrends(userId, 6, null);

            // Assert
            assertThat(trends).hasSize(6);
            assertThat(trends).allMatch(t -> t.month().contains("-"));
        }
    }

    @Nested
    @DisplayName("getSpendingComparison")
    class GetSpendingComparisonTests {

        @Test
        @DisplayName("Should compare current vs previous month with default period")
        void shouldCompareCurrentVsPreviousMonth_WithDefaultPeriod() {
            // Arrange
            String userId = "user-123";
            LocalDate now = LocalDate.now();
            LocalDate prevMonth = now.minusMonths(1);

            List<Transaction> currentTxns = List.of(
                    createTransaction("t1", userId, now, new BigDecimal("500.00"), "DEBIT", "FOOD", "Swiggy")
            );
            List<Transaction> prevTxns = List.of(
                    createTransaction("t2", userId, prevMonth, new BigDecimal("300.00"), "DEBIT", "FOOD", "Zomato")
            );

            given(transactionRepository.findByUserIdAndTransactionDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(currentTxns)
                    .willReturn(prevTxns);

            // Act
            var comparison = sut.getSpendingComparison(userId, null);

            // Assert
            assertThat(comparison).isNotNull();
            assertThat(comparison.currentTotal()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(comparison.previousTotal()).isEqualByComparingTo(new BigDecimal("300.00"));
        }

        @Test
        @DisplayName("Should show increase when current period spending is higher")
        void shouldShowIncrease_WhenCurrentPeriodSpendingHigher() {
            // Arrange
            String userId = "user-123";
            LocalDate now = LocalDate.now();
            LocalDate prevMonth = now.minusMonths(1);

            List<Transaction> currentTxns = List.of(
                    createTransaction("t1", userId, now, new BigDecimal("800.00"), "DEBIT", "FOOD", "Swiggy")
            );
            List<Transaction> prevTxns = List.of(
                    createTransaction("t2", userId, prevMonth, new BigDecimal("500.00"), "DEBIT", "FOOD", "Zomato")
            );

            given(transactionRepository.findByUserIdAndTransactionDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(currentTxns)
                    .willReturn(prevTxns);

            // Act
            var comparison = sut.getSpendingComparison(userId, null);

            // Assert
            assertThat(comparison.changeAmount()).isPositive();
            assertThat(comparison.changePercentage()).isPositive();
            assertThat(comparison.changeAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        }

        @Test
        @DisplayName("Should show decrease when current period spending is lower")
        void shouldShowDecrease_WhenCurrentPeriodSpendingLower() {
            // Arrange
            String userId = "user-123";
            LocalDate now = LocalDate.now();
            LocalDate prevMonth = now.minusMonths(1);

            List<Transaction> currentTxns = List.of(
                    createTransaction("t1", userId, now, new BigDecimal("200.00"), "DEBIT", "FOOD", "Swiggy")
            );
            List<Transaction> prevTxns = List.of(
                    createTransaction("t2", userId, prevMonth, new BigDecimal("600.00"), "DEBIT", "FOOD", "Zomato")
            );

            given(transactionRepository.findByUserIdAndTransactionDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(currentTxns)
                    .willReturn(prevTxns);

            // Act
            var comparison = sut.getSpendingComparison(userId, null);

            // Assert
            assertThat(comparison.changeAmount()).isNegative();
            assertThat(comparison.changePercentage()).isNegative();
        }

        @Test
        @DisplayName("Should return category-level changes in comparison")
        void shouldReturnCategoryLevelChanges_InComparison() {
            // Arrange
            String userId = "user-123";
            LocalDate now = LocalDate.now();
            LocalDate prevMonth = now.minusMonths(1);

            List<Transaction> currentTxns = List.of(
                    createTransaction("t1", userId, now, new BigDecimal("400.00"), "DEBIT", "FOOD", "Swiggy"),
                    createTransaction("t2", userId, now, new BigDecimal("200.00"), "DEBIT", "TRANSPORT", "Uber")
            );
            List<Transaction> prevTxns = List.of(
                    createTransaction("t3", userId, prevMonth, new BigDecimal("300.00"), "DEBIT", "FOOD", "Zomato"),
                    createTransaction("t4", userId, prevMonth, new BigDecimal("100.00"), "DEBIT", "TRANSPORT", "Ola")
            );

            given(transactionRepository.findByUserIdAndTransactionDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(currentTxns)
                    .willReturn(prevTxns);

            // Act
            var comparison = sut.getSpendingComparison(userId, null);

            // Assert
            assertThat(comparison.categoryComparisons()).isNotEmpty();
            assertThat(comparison.categoryComparisons()).anyMatch(c -> "FOOD".equals(c.category()));
            assertThat(comparison.categoryComparisons()).anyMatch(c -> "TRANSPORT".equals(c.category()));
        }

        @Test
        @DisplayName("Should handle gracefully when no transactions")
        void shouldHandleGracefully_WhenNoTransactions() {
            // Arrange
            String userId = "user-123";
            given(transactionRepository.findByUserIdAndTransactionDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(List.of());

            // Act
            var comparison = sut.getSpendingComparison(userId, null);

            // Assert
            assertThat(comparison).isNotNull();
            assertThat(comparison.currentTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(comparison.previousTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(comparison.changeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
