package com.tracker.MoneyTracker.ai.dto;

import java.math.BigDecimal;

/**
 * Affordability analysis result.
 *
 * @param userId     the user ID
 * @param itemName   the item being evaluated
 * @param cost       the item cost
 * @param affordable whether the user can afford the item
 * @param analysis   AI-generated explanation of the affordability
 */
public record AffordabilityResponse(
        String userId,
        String itemName,
        BigDecimal cost,
        boolean affordable,
        String analysis
) {
}
