package com.tracker.MoneyTracker.ai.dto;

import java.util.List;

/**
 * AI-generated financial recommendations for a user.
 *
 * @param userId          the user ID
 * @param recommendations list of personalized recommendation strings
 */
public record RecommendationResponse(
        String userId,
        List<String> recommendations
) {
}
