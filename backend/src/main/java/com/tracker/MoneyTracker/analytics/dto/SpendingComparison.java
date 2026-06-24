package com.tracker.MoneyTracker.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Comparison of spending between current and previous period.
 *
 * @param currentPeriod        the current period label (e.g. "2026-06")
 * @param previousPeriod       the previous period label (e.g. "2026-05")
 * @param currentTotal         total spending in the current period
 * @param previousTotal        total spending in the previous period
 * @param changeAmount         difference (current - previous), positive = increase
 * @param changePercentage     percentage change, positive = increase
 * @param categoryComparisons  per-category spending changes
 */
public record SpendingComparison(
        String currentPeriod,
        String previousPeriod,
        BigDecimal currentTotal,
        BigDecimal previousTotal,
        BigDecimal changeAmount,
        double changePercentage,
        List<CategoryChange> categoryComparisons
) {
}
