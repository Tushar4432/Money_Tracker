package com.tracker.MoneyTracker.ai.dto;

/**
 * Financial health score for a user (0-100).
 *
 * @param userId        the user ID
 * @param score         overall health score from 0 to 100
 * @param savingsRate   savings rate percentage
 * @param goalProgress  average goal completion percentage
 * @param breakdown     human-readable explanation of the score
 */
public record HealthScoreResponse(
        String userId,
        int score,
        double savingsRate,
        double goalProgress,
        String breakdown
) {
}
