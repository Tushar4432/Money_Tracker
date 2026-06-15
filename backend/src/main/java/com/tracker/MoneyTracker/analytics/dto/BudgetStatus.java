package com.tracker.MoneyTracker.analytics.dto;

import java.math.BigDecimal;

/**
 * Budget goal progress for a category.
 *
 * @param category       the spending category
 * @param targetAmount   the budget target amount
 * @param spentAmount    actual amount spent in the period
 * @param remainingAmount remaining budget (target - spent), can be negative
 * @param percentageUsed percentage of budget used (0-100+)
 * @param period         the goal period (WEEKLY, MONTHLY, QUARTERLY, YEARLY)
 */
public record BudgetStatus(String category, BigDecimal targetAmount, BigDecimal spentAmount,
                           BigDecimal remainingAmount, double percentageUsed, String period) {
}
