package com.tracker.MoneyTracker.ai.service;

import com.tracker.MoneyTracker.ai.client.OllamaClient;
import com.tracker.MoneyTracker.ai.dto.AffordabilityResponse;
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
 * Analyzes whether a user can afford a specific purchase based on their
 * financial data. Combines deterministic checks with AI-generated advice.
 */
@Service
public class AffordabilityService {

    private static final Logger log = LoggerFactory.getLogger(AffordabilityService.class);

    /** Percentage of monthly income above which a purchase is considered expensive. */
    private static final double EXPENSE_THRESHOLD_PERCENT = 10.0;

    private final OllamaClient ollamaClient;
    private final AnalyticsService analyticsService;
    private final GoalService goalService;

    public AffordabilityService(OllamaClient ollamaClient,
                               AnalyticsService analyticsService,
                               GoalService goalService) {
        this.ollamaClient = ollamaClient;
        this.analyticsService = analyticsService;
        this.goalService = goalService;
    }

    /**
     * Analyzes whether the user can afford the given item.
     *
     * @param userId   the user ID
     * @param itemName description of the item
     * @param cost     cost of the item
     * @return affordability analysis
     */
    public AffordabilityResponse analyze(String userId, String itemName, BigDecimal cost) {
        SpendingSummary summary = analyticsService.getSpendingSummary(userId);

        // Deterministic affordability check
        boolean affordable = isAffordable(cost, summary);

        // Build context for LLM
        String prompt = buildAffordabilityPrompt(userId, itemName, cost, summary);
        String analysis = ollamaClient.generate(prompt);

        return new AffordabilityResponse(userId, itemName, cost, affordable, analysis);
    }

    /**
     * Deterministic check: a purchase is considered affordable if:
     * 1. The user has positive net savings, AND
     * 2. The cost is less than their monthly net savings, OR
     * 3. The cost is less than EXPENSE_THRESHOLD_PERCENT of monthly income
     */
    private boolean isAffordable(BigDecimal cost, SpendingSummary summary) {
        if (summary.netSavings() == null || summary.netSavings().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        // Can afford if cost <= monthly net savings
        if (cost.compareTo(summary.netSavings()) <= 0) {
            return true;
        }

        // Can afford if cost is under threshold % of income
        if (summary.totalIncome() != null && summary.totalIncome().compareTo(BigDecimal.ZERO) > 0) {
            double costPercent = cost.multiply(BigDecimal.valueOf(100))
                    .divide(summary.totalIncome(), 2, RoundingMode.HALF_UP)
                    .doubleValue();
            return costPercent < EXPENSE_THRESHOLD_PERCENT;
        }

        return false;
    }

    private String buildAffordabilityPrompt(String userId, String itemName, BigDecimal cost, SpendingSummary summary) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a personal financial coach. A user wants to know if they can afford a purchase.\n\n");

        prompt.append("Item: ").append(itemName).append("\n");
        prompt.append("Cost: ").append(cost).append("\n\n");

        prompt.append("User's Financial Data:\n");
        prompt.append("- Total Income: ").append(summary.totalIncome()).append("\n");
        prompt.append("- Total Expenses: ").append(summary.totalExpense()).append("\n");
        prompt.append("- Net Savings: ").append(summary.netSavings()).append("\n");
        prompt.append("- Top Spending Category: ").append(summary.topCategory()).append("\n\n");

        try {
            List<GoalProgress> goals = goalService.evaluateAllGoals(userId);
            if (!goals.isEmpty()) {
                prompt.append("Active Goals:\n");
                for (GoalProgress goal : goals) {
                    prompt.append(String.format("- Goal %s: %.1f%% of budget used\n",
                            goal.goalId(), goal.percentageUsed()));
                }
                prompt.append("\n");
            }
        } catch (Exception e) {
            log.warn("Could not fetch goals for affordability analysis", e);
        }

        prompt.append("Provide a concise analysis (2-3 paragraphs):\n");
        prompt.append("1. Can they afford it right now?\n");
        prompt.append("2. What impact would this purchase have on their goals?\n");
        prompt.append("3. Any suggestions for timing or alternatives?\n");

        return prompt.toString();
    }
}
