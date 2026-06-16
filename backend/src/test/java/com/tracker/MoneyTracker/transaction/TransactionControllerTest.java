package com.tracker.MoneyTracker.transaction;

import com.tracker.MoneyTracker.exception.BadRequestException;
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
            MockMultipartFile file = new MockMultipartFile(
                    "bank_statements", "statement.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "test content".getBytes()
            );

            ResponseEntity<?> result = sut.uploadStatement(file, "user-123");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @Test
        @DisplayName("Should accept CSV file upload and return created status")
        void shouldAcceptCsvUpload_AndReturnCreated() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "bank_statements", "statement.csv",
                    "text/csv",
                    "Date,Details,Debit,Credit\n".getBytes()
            );

            ResponseEntity<?> result = sut.uploadStatement(file, "user-123");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @Test
        @DisplayName("Should throw BadRequestException when file is empty")
        void shouldThrowBadRequest_WhenFileIsEmpty() {
            MockMultipartFile file = new MockMultipartFile(
                    "bank_statements", "empty.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    new byte[0]
            );

            assertThatThrownBy(() -> sut.uploadStatement(file, "user-123"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should throw BadRequestException when file type is unsupported")
        void shouldThrowBadRequest_WhenUnsupportedFileType() {
            MockMultipartFile file = new MockMultipartFile(
                    "bank_statements", "statement.pdf",
                    "application/pdf",
                    "test".getBytes()
            );

            assertThatThrownBy(() -> sut.uploadStatement(file, "user-123"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should throw BadRequestException when userId is blank")
        void shouldThrowBadRequest_WhenUserIdIsBlank() {
            MockMultipartFile file = new MockMultipartFile(
                    "bank_statements", "statement.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "test".getBytes()
            );

            assertThatThrownBy(() -> sut.uploadStatement(file, ""))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("getTransactions")
    class GetTransactionsTests {

        @Test
        @DisplayName("Should return transactions for user")
        void shouldReturnTransactions_ForUser() {
            List<Transaction> transactions = List.of();
            given(transactionService.getTransactionsByUser("user-123")).willReturn(transactions);

            ResponseEntity<List<Transaction>> result = sut.getTransactions("user-123");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
        }

        @Test
        @DisplayName("Should throw BadRequestException when userId is blank")
        void shouldThrowBadRequest_WhenUserIdIsBlank() {
            assertThatThrownBy(() -> sut.getTransactions(""))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("getCategorySummary")
    class GetCategorySummaryTests {

        @Test
        @DisplayName("Should return category summary for user")
        void shouldReturnCategorySummary_ForUser() {
            Map<String, BigDecimal> summary = Map.of("FOOD", new BigDecimal("1000.00"));
            given(transactionService.getCategorySummary("user-123")).willReturn(summary);

            ResponseEntity<Map<String, BigDecimal>> result = sut.getCategorySummary("user-123");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
        }

        @Test
        @DisplayName("Should throw BadRequestException when userId is blank")
        void shouldThrowBadRequest_WhenUserIdIsBlank() {
            assertThatThrownBy(() -> sut.getCategorySummary(""))
                    .isInstanceOf(BadRequestException.class);
        }
    }
}
