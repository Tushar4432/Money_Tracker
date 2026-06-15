package com.tracker.MoneyTracker.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionController")
class TransactionControllerTest {

    private TransactionController sut;

    @Mock
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        sut = new TransactionController(transactionService);
    }

    @Nested
    @DisplayName("uploadStatement")
    class UploadStatementTests {

        @Test
        @DisplayName("Should accept Excel file upload and return created status")
        void shouldAcceptExcelUpload_AndReturnCreated() throws Exception {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file", "statement.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "test content".getBytes()
            );

            // Act
            ResponseEntity<?> result = sut.uploadStatement(file, "user-123");

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @Test
        @DisplayName("Should accept CSV file upload and return created status")
        void shouldAcceptCsvUpload_AndReturnCreated() throws Exception {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file", "statement.csv",
                    "text/csv",
                    "Date,Details,Debit,Credit\n".getBytes()
            );

            // Act
            ResponseEntity<?> result = sut.uploadStatement(file, "user-123");

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @Test
        @DisplayName("Should return bad request when file is empty")
        void shouldReturnBadRequest_WhenFileIsEmpty() {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file", "empty.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    new byte[0]
            );

            // Act
            ResponseEntity<?> result = sut.uploadStatement(file, "user-123");

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return bad request when file type is unsupported")
        void shouldReturnBadRequest_WhenUnsupportedFileType() {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file", "statement.pdf",
                    "application/pdf",
                    "test".getBytes()
            );

            // Act
            ResponseEntity<?> result = sut.uploadStatement(file, "user-123");

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return bad request when userId is blank")
        void shouldReturnBadRequest_WhenUserIdIsBlank() {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file", "statement.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "test".getBytes()
            );

            // Act
            ResponseEntity<?> result = sut.uploadStatement(file, "");

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("getTransactions")
    class GetTransactionsTests {

        @Test
        @DisplayName("Should return transactions for user")
        void shouldReturnTransactions_ForUser() {
            // Arrange
            List<Transaction> transactions = List.of();
            given(transactionService.getTransactionsByUser("user-123")).willReturn(transactions);

            // Act
            ResponseEntity<List<Transaction>> result = sut.getTransactions("user-123");

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
        }

        @Test
        @DisplayName("Should return bad request when userId is blank")
        void shouldReturnBadRequest_WhenUserIdIsBlank() {
            ResponseEntity<List<Transaction>> result = sut.getTransactions("");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("getCategorySummary")
    class GetCategorySummaryTests {

        @Test
        @DisplayName("Should return category summary for user")
        void shouldReturnCategorySummary_ForUser() {
            // Arrange
            Map<String, BigDecimal> summary = Map.of("FOOD", new BigDecimal("1000.00"));
            given(transactionService.getCategorySummary("user-123")).willReturn(summary);

            // Act
            ResponseEntity<Map<String, BigDecimal>> result = sut.getCategorySummary("user-123");

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
        }

        @Test
        @DisplayName("Should return bad request when userId is blank")
        void shouldReturnBadRequest_WhenUserIdIsBlank() {
            ResponseEntity<Map<String, BigDecimal>> result = sut.getCategorySummary("");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
