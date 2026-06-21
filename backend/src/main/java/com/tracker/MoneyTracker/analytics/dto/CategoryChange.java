package com.tracker.MoneyTracker.analytics.dto;

import java.math.BigDecimal;

/**
 * Per-category spending change between two periods.
 *
 * @param category        the spending category
 * @param currentAmount   amount spent in the current period
 * @param previousAmount  amount spent in the previous period
 * @param changeAmount    difference (current - previous), positive = increase
 * @param changePercentage percentage change, positive = increase
 */
public record CategoryChange(
        String category,
        BigDecimal currentAmount,
        BigDecimal previousAmount,
        BigDecimal changeAmount,
        double changePercentage
) {
}
