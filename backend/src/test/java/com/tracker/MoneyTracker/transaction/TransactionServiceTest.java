package com.tracker.MoneyTracker.transaction;

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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService")
class TransactionServiceTest {

    private TransactionService sut;

    @BeforeEach
    void setUp() {
        sut = new TransactionService();
    }

    @Nested
    @DisplayName("processStatement")
    class ProcessStatementTests {

        @Test
        @DisplayName("Should parse and return transactions when valid file provided")
        void shouldParseAndReturnTransactions_WhenValidFileProvided() throws Exception {
            // Arrange
            java.io.File file = new java.io.File("src/test/resources/test-statement.xlsx");
            String userId = "user-123";

            // Act
            List<Transaction> result = sut.processStatement(file, userId);

            // Assert
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(317);
        }

        @Test
        @DisplayName("Should set userId on all transactions")
        void shouldSetUserIdOnAllTransactions() throws Exception {
            // Arrange
            java.io.File file = new java.io.File("src/test/resources/test-statement.xlsx");
            String userId = "user-456";

            // Act
            List<Transaction> result = sut.processStatement(file, userId);

            // Assert
            assertThat(result).allMatch(t -> userId.equals(t.getUserId()));
        }

        @Test
        @DisplayName("Should set DEBIT type when debit amount present")
        void shouldSetDebitType_WhenDebitAmountPresent() throws Exception {
            // Arrange
            java.io.File file = new java.io.File("src/test/resources/test-statement.xlsx");
            String userId = "user-123";

            // Act
            List<Transaction> result = sut.processStatement(file, userId);

            // Assert
            assertThat(result).anyMatch(t -> "DEBIT".equals(t.getType()));
        }

        @Test
        @DisplayName("Should set CREDIT type when credit amount present")
        void shouldSetCreditType_WhenCreditAmountPresent() throws Exception {
            // Arrange
            java.io.File file = new java.io.File("src/test/resources/test-statement.xlsx");
            String userId = "user-123";

            // Act
            List<Transaction> result = sut.processStatement(file, userId);

            // Assert
            assertThat(result).anyMatch(t -> "CREDIT".equals(t.getType()));
        }

        @Test
        @DisplayName("Should parse amount as positive BigDecimal")
        void shouldParseAmountAsPositiveBigDecimal() throws Exception {
            // Arrange
            java.io.File file = new java.io.File("src/test/resources/test-statement.xlsx");
            String userId = "user-123";

            // Act
            List<Transaction> result = sut.processStatement(file, userId);

            // Assert
            assertThat(result).allMatch(t -> t.getAmount() != null && t.getAmount().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("Should set description from details column")
        void shouldSetDescriptionFromDetailsColumn() throws Exception {
            // Arrange
            java.io.File file = new java.io.File("src/test/resources/test-statement.xlsx");
            String userId = "user-123";

            // Act
            List<Transaction> result = sut.processStatement(file, userId);

            // Assert
            assertThat(result).allMatch(t -> t.getDescription() != null && !t.getDescription().isBlank());
        }

        @Test
        @DisplayName("Should classify each transaction with a category")
        void shouldClassifyEachTransaction() throws Exception {
            // Arrange
            java.io.File file = new java.io.File("src/test/resources/test-statement.xlsx");
            String userId = "user-123";

            // Act
            List<Transaction> result = sut.processStatement(file, userId);

            // Assert
            assertThat(result).allMatch(t -> t.getCategory() != null && !t.getCategory().isBlank());
        }

        @Test
        @DisplayName("Should parse transaction date")
        void shouldParseTransactionDate() throws Exception {
            // Arrange
            java.io.File file = new java.io.File("src/test/resources/test-statement.xlsx");
            String userId = "user-123";

            // Act
            List<Transaction> result = sut.processStatement(file, userId);

            // Assert
            assertThat(result).allMatch(t -> t.getTransactionDate() != null);
        }

        @Test
        @DisplayName("Should throw exception when file is null")
        void shouldThrowException_WhenFileIsNull() {
            assertThatThrownBy(() -> sut.processStatement(null, "user-123"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw exception when userId is null")
        void shouldThrowException_WhenUserIdIsNull() throws Exception {
            java.io.File file = new java.io.File("src/test/resources/test-statement.xlsx");
            assertThatThrownBy(() -> sut.processStatement(file, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw exception when userId is blank")
        void shouldThrowException_WhenUserIdIsBlank() throws Exception {
            java.io.File file = new java.io.File("src/test/resources/test-statement.xlsx");
            assertThatThrownBy(() -> sut.processStatement(file, "  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("getTransactionsByUser")
    class GetTransactionsByUserTests {

        @Test
        @DisplayName("Should return empty list when no transactions for user")
        void shouldReturnEmptyList_WhenNoTransactions() {
            // Act
            List<Transaction> result = sut.getTransactionsByUser("nonexistent-user");

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getTransactionsByUserAndDateRange")
    class GetTransactionsByUserAndDateRangeTests {

        @Test
        @DisplayName("Should return transactions within date range")
        void shouldReturnTransactions_WithinDateRange() {
            // Arrange
            String userId = "user-123";
            LocalDate start = LocalDate.of(2026, 3, 1);
            LocalDate end = LocalDate.of(2026, 3, 31);

            // Act
            List<Transaction> result = sut.getTransactionsByUserAndDateRange(userId, start, end);

            // Assert
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when startDate is after endDate")
        void shouldThrowException_WhenStartDateAfterEndDate() {
            LocalDate start = LocalDate.of(2026, 3, 31);
            LocalDate end = LocalDate.of(2026, 3, 1);

            assertThatThrownBy(() -> sut.getTransactionsByUserAndDateRange("user-123", start, end))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("getCategorySummary")
    class GetCategorySummaryTests {

        @Test
        @DisplayName("Should return category summary for user")
        void shouldReturnCategorySummary_ForUser() {
            // Act
            Map<String, BigDecimal> result = sut.getCategorySummary("user-123");

            // Assert
            assertThat(result).isNotNull();
        }
    }
}
