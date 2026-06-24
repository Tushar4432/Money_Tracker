package com.tracker.MoneyTracker.analytics.dto;

import java.math.BigDecimal;

/**
 * Breakdown of spending for a single category.
 *
 * @param category   the category name (e.g. "FOOD", "TRANSPORT")
 * @param amount     total amount spent in this category
 * @param percentage percentage of total spending this category represents
 */
public record CategoryBreakdown(String category, BigDecimal amount, double percentage) {
}
