package com.tracker.MoneyTracker.goal;

import java.math.BigDecimal;

/**
 * Progress report for a spending goal.
 *
 * @param goalId        the goal ID
 * @param category      the spending category
 * @param targetAmount  the budget target amount
 * @param spentAmount   actual amount spent in the goal period
 * @param remainingAmount remaining budget (target - spent), can be negative if over budget
 * @param percentageUsed percentage of budget used (0-100+)
 * @param period        the goal period (WEEKLY, MONTHLY, QUARTERLY, YEARLY)
 */
public record GoalProgress(String goalId, String category, BigDecimal targetAmount,
                           BigDecimal spentAmount, BigDecimal remainingAmount,
                           double percentageUsed, String period) {
}
