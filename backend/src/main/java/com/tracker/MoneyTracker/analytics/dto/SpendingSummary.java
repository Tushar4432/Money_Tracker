package com.tracker.MoneyTracker.analytics.dto;

import java.math.BigDecimal;

/**
 * Overall spending summary for a user.
 *
 * @param totalIncome      total credit amount across all transactions
 * @param totalExpense     total debit amount across all transactions
 * @param netSavings       net savings (income - expense)
 * @param topCategory      the category with the highest spending
 * @param transactionCount total number of transactions
 */
public record SpendingSummary(BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal netSavings,
                              String topCategory, int transactionCount) {
}
