package com.tracker.MoneyTracker.transaction;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_transactions")
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
