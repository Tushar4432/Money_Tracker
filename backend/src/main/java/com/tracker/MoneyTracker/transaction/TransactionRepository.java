package com.tracker.MoneyTracker.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByUserId(String userId);

    List<Transaction> findByUserIdAndTransactionDateBetween(String userId, LocalDate start, LocalDate end);

    /**
     * Aggregate total income (CREDIT) for a user — computed in the database,
     * avoiding loading all transactions into memory.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.type = 'CREDIT'")
    BigDecimal sumIncomeByUserId(@Param("userId") String userId);

    /**
     * Aggregate total expenses (DEBIT) for a user — computed in the database.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.type = 'DEBIT'")
    BigDecimal sumExpenseByUserId(@Param("userId") String userId);

    /**
     * Count all transactions for a user — computed in the database.
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.userId = :userId")
    int countByUserId(@Param("userId") String userId);

    /**
     * Find the top spending category for a user by summing DEBIT amounts per category.
     * Returns a list of Object[] with [category, totalAmount], sorted descending, limited to 1.
     */
    @Query("SELECT t.category, SUM(t.amount) as total FROM Transaction t " +
           "WHERE t.userId = :userId AND t.type = 'DEBIT' AND t.category IS NOT NULL " +
           "GROUP BY t.category ORDER BY total DESC LIMIT 1")
    List<Object[]> findTopCategoryByUserId(@Param("userId") String userId);
}
