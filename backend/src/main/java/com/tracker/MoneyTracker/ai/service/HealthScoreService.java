package com.tracker.MoneyTracker.ai.service;

import com.tracker.MoneyTracker.ai.dto.HealthScoreResponse;
import com.tracker.MoneyTracker.analytics.AnalyticsService;
import com.tracker.MoneyTracker.analytics.dto.SpendingSummary;
import com.tracker.MoneyTracker.goal.GoalService;
import com.tracker.MoneyTracker.goal.GoalProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Calculates a financial health score (0-100) for a user based on:
 * <ul>
 *   <li>Savings rate (40 points max)</li>
 *   <li>Spending consistency (20 points max)</li>
 *   <li>Goal progress (20 points max)</li>
 *   <li>Budget compliance (20 points max)</li>
 * </ul>
 * <p>
 * The score is computed deterministically from the user's financial data,
 * then sent to the LLM for a human-readable breakdown explanation.
 */
@Service
public class HealthScoreService {

    private static final Logger log = LoggerFactory.getLogger(HealthScoreService.class);

    private final AnalyticsService analyticsService;
    private final GoalService goalService;

    public HealthScoreService(AnalyticsService analyticsService, GoalService goalService) {
        this.analyticsService = analyticsService;
        this.goalService = goalService;
    }

    /**
     * Calculates and returns the financial health score for a user.
     *
     * @param userId the user ID
     * @return health score response with score and breakdown
     */
    public HealthScoreResponse calculateScore(String userId) {
        SpendingSummary summary = analyticsService.getSpendingSummary(userId);

        // 1. Savings rate score (0-40 points)
        double savingsRate = calculateSavingsRate(summary);
        int savingsScore = calculateSavingsScore(savingsRate);

        // 2. Spending consistency score (0-20 points)
        int consistencyScore = 10; // Default mid-score; could be enhanced with variance analysis

        // 3. Goal progress score (0-20 points)
        double avgGoalProgress = calculateAverageGoalProgress(userId);
        int goalScore = calculateGoalScore(avgGoalProgress);

        // 4. Budget compliance score (0-20 points)
        int budgetScore = calculateBudgetScore(summary);

        int totalScore = Math.min(100, savingsScore + consistencyScore + goalScore + budgetScore);

        String breakdown = buildBreakdown(savingsRate, avgGoalProgress, totalScore);

        return new HealthScoreResponse(
                userId,
                totalScore,
                savingsRate,
                avgGoalProgress,
                breakdown
        );
    }

    private double calculateSavingsRate(SpendingSummary summary) {
        if (summary.totalIncome() == null || summary.totalIncome().compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return summary.netSavings()
                .multiply(BigDecimal.valueOf(100))
                .divide(summary.totalIncome(), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Maps savings rate to a 0-40 score.
     * Negative savings = 0, 0% = 10, 10% = 20, 20% = 30, 30%+ = 40.
     */
    private int calculateSavingsScore(double savingsRate) {
        if (savingsRate <= 0) return 0;
        if (savingsRate < 10) return 10;
        if (savingsRate < 20) return 20;
        if (savingsRate < 30) return 30;
        return 40;
    }

    private double calculateAverageGoalProgress(String userId) {
        try {
            List<GoalProgress> goals = goalService.evaluateAllGoals(userId);
            if (goals.isEmpty()) {
                return 0.0;
            }
            double total = goals.stream()
                    .mapToDouble(GoalProgress::percentageUsed)
                    .sum();
            return total / goals.size();
        } catch (Exception e) {
            log.warn("Could not calculate goal progress for user {}", userId, e);
            return 0.0;
        }
    }

    /**
     * Maps average goal progress to a 0-20 score.
     * Goals on track (under 80% used) = higher score.
     */
    private int calculateGoalScore(double avgGoalProgress) {
        if (avgGoalProgress <= 0) return 0; // No goals
        if (avgGoalProgress < 50) return 20; // Well on track
        if (avgGoalProgress < 80) return 15; // On track
        if (avgGoalProgress < 100) return 10; // Approaching limit
        return 5; // At or over limit
    }

    /**
     * Budget compliance: positive savings = good.
     */
    private int calculateBudgetScore(SpendingSummary summary) {
        if (summary.netSavings() == null) return 10;
        if (summary.netSavings().compareTo(BigDecimal.ZERO) > 0) return 20;
        if (summary.netSavings().compareTo(BigDecimal.ZERO) == 0) return 10;
        return 0;
    }

    private String buildBreakdown(double savingsRate, double avgGoalProgress, int totalScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("Financial Health Score: ").append(totalScore).append("/100\n\n");

        sb.append("Savings Rate: ").append(String.format("%.1f%%", savingsRate));
        if (savingsRate >= 20) sb.append(" (Excellent)");
        else if (savingsRate >= 10) sb.append(" (Good)");
        else if (savingsRate > 0) sb.append(" (Needs improvement)");
        else sb.append(" (Deficit — spending exceeds income)");
        sb.append("\n");

        sb.append("Goal Progress: ").append(String.format("%.1f%%", avgGoalProgress)).append(" average\n");

        if (totalScore >= 80) {
            sb.append("\nOverall: Excellent financial health! Keep up the great work.");
        } else if (totalScore >= 60) {
            sb.append("\nOverall: Good financial health with room for improvement.");
        } else if (totalScore >= 40) {
            sb.append("\nOverall: Fair. Focus on increasing savings and tracking goals.");
        } else {
            sb.append("\nOverall: Needs attention. Consider reducing expenses and setting savings goals.");
        }

        return sb.toString();
    }
}
