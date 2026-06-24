package com.tracker.MoneyTracker.transaction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Transaction")
class TransactionTest {

    @Nested
    @DisplayName("creation")
    class CreationTests {

        @Test
        @DisplayName("Should create transaction with all fields populated")
        void shouldCreateTransaction_WithAllFields() {
            // Arrange & Act
            Transaction tx = new Transaction();
            tx.setId("tx-001");
            tx.setUserId("user-123");
            tx.setTransactionDate(LocalDate.of(2026, 3, 1));
            tx.setAmount(new BigDecimal("646.00"));
            tx.setType("DEBIT");
            tx.setDescription("UPI/DR/SU BLR S/HDFC/SUBLRSARJ");
            tx.setCategory("FOOD");
            tx.setSentTo("SU BLR S");
            tx.setOriginalDetail("WDL TFR UPI/DR/580319350694/SU BLR S/HDFC/SUBLRSARJ");
            tx.setCreatedAt(LocalDateTime.now());
            tx.setUpdatedAt(LocalDateTime.now());

            // Assert
            assertThat(tx.getId()).isEqualTo("tx-001");
            assertThat(tx.getUserId()).isEqualTo("user-123");
            assertThat(tx.getTransactionDate()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("646.00"));
            assertThat(tx.getType()).isEqualTo("DEBIT");
            assertThat(tx.getDescription()).isEqualTo("UPI/DR/SU BLR S/HDFC/SUBLRSARJ");
            assertThat(tx.getCategory()).isEqualTo("FOOD");
            assertThat(tx.getSentTo()).isEqualTo("SU BLR S");
            assertThat(tx.getOriginalDetail()).isEqualTo("WDL TFR UPI/DR/580319350694/SU BLR S/HDFC/SUBLRSARJ");
            assertThat(tx.getCreatedAt()).isNotNull();
            assertThat(tx.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should create transaction with minimal required fields")
        void shouldCreateTransaction_WithMinimalFields() {
            // Arrange & Act
            Transaction tx = new Transaction();
            tx.setId("tx-002");
            tx.setUserId("user-456");
            tx.setAmount(new BigDecimal("100.00"));
            tx.setType("CREDIT");

            // Assert
            assertThat(tx.getId()).isEqualTo("tx-002");
            assertThat(tx.getUserId()).isEqualTo("user-456");
            assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(tx.getType()).isEqualTo("CREDIT");
        }

        @Test
        @DisplayName("Should allow null optional fields")
        void shouldAllowNullOptionalFields() {
            // Arrange & Act
            Transaction tx = new Transaction();
            tx.setId("tx-003");
            tx.setUserId("user-789");
            tx.setAmount(new BigDecimal("50.00"));
            tx.setType("DEBIT");

            // Assert
            assertThat(tx.getTransactionDate()).isNull();
            assertThat(tx.getDescription()).isNull();
            assertThat(tx.getCategory()).isNull();
            assertThat(tx.getSentTo()).isNull();
            assertThat(tx.getOriginalDetail()).isNull();
            assertThat(tx.getCreatedAt()).isNull();
            assertThat(tx.getUpdatedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("type validation")
    class TypeValidationTests {

        @Test
        @DisplayName("Should accept DEBIT as valid type")
        void shouldAcceptDebit() {
            Transaction tx = new Transaction();
            tx.setType("DEBIT");
            assertThat(tx.getType()).isEqualTo("DEBIT");
        }

        @Test
        @DisplayName("Should accept CREDIT as valid type")
        void shouldAcceptCredit() {
            Transaction tx = new Transaction();
            tx.setType("CREDIT");
            assertThat(tx.getType()).isEqualTo("CREDIT");
        }
    }
}
