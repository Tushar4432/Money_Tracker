package com.tracker.MoneyTracker.goal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "spending_goals")
@Getter
@Setter
public class SpendingGoal {

    @Id
    private String id;
    private String userId;
    private String category;
    private BigDecimal targetAmount;
    private String period;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
