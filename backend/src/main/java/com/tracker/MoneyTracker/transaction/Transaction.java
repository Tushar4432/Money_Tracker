package com.tracker.MoneyTracker.transaction;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_transactions", indexes = {
    @Index(name = "idx_tx_user_hash", columnList = "userId, transactionHash")
})
@Getter
@Setter
public class Transaction {

    @Id
    private String id;
    private String userId;
    private LocalDate transactionDate;
    private BigDecimal amount;
    private String type;
    private String description;
    private String category;
    private String sentTo;
    private String originalDetail;

    /**
     * SHA-256 hash of (userId + date + amount + type + originalDetail).
     * Used for O(1) duplicate detection when re-uploading bank statements.
     */
    @Column(name = "transaction_hash", length = 64)
    private String transactionHash;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (transactionHash == null) {
            transactionHash = TransactionHashUtil.compute(userId, transactionDate, amount, type, originalDetail);
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
