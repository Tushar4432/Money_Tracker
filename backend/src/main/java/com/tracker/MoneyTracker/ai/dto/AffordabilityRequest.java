package com.tracker.MoneyTracker.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request body for the affordability analysis endpoint.
 *
 * @param userId   the authenticated user's ID
 * @param itemName name/description of the item to check
 * @param cost     cost of the item
 */
public record AffordabilityRequest(
        @NotNull(message = "userId is required") String userId,
        @NotBlank(message = "itemName is required") String itemName,
        @NotNull(message = "cost is required") @Positive(message = "cost must be positive") BigDecimal cost
) {
}
