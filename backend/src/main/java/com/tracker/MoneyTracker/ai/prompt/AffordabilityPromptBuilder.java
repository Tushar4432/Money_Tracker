package com.tracker.MoneyTracker.ai.prompt;

import com.tracker.MoneyTracker.analytics.dto.SpendingSummary;
import com.tracker.MoneyTracker.goal.GoalProgress;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Builds prompts for the affordability analysis feature.
 * <p>
 * Constructs a structured prompt that asks the LLM to evaluate whether
 * a user can afford a specific purchase given their financial data.
 */
@Component
public class AffordabilityPromptBuilder {

    /**
     * Builds the full affordability analysis prompt.
     *
     * @param itemName description of the item
     * @param cost     cost of the item
     * @param summary  the user's spending summary
     * @param goals    the user's active goal progress
     * @return the complete prompt for the LLM
     */
    public String buildPrompt(String itemName, BigDecimal cost,
                              SpendingSummary summary, List<GoalProgress> goals) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a personal financial coach. A user wants to know if they can afford a purchase.\n\n");

        prompt.append("Item: ").append(itemName).append("\n");
        prompt.append("Cost: ").append(cost).append("\n\n");

        prompt.append("User's Financial Data:\n");
        prompt.append("- Total Income: ").append(summary.totalIncome()).append("\n");
        prompt.append("- Total Expenses: ").append(summary.totalExpense()).append("\n");
        prompt.append("- Net Savings: ").append(summary.netSavings()).append("\n");
        prompt.append("- Top Spending Category: ").append(summary.topCategory()).append("\n\n");

        if (goals != null && !goals.isEmpty()) {
            prompt.append("Active Goals:\n");
            for (GoalProgress goal : goals) {
                prompt.append(String.format("- Goal %s: %.1f%% of budget used\n",
                        goal.goalId(), goal.percentageUsed()));
            }
            prompt.append("\n");
        }

        prompt.append("Provide a concise analysis (2-3 paragraphs):\n");
        prompt.append("1. Can they afford it right now?\n");
        prompt.append("2. What impact would this purchase have on their goals?\n");
        prompt.append("3. Any suggestions for timing or alternatives?\n");

        return prompt.toString();
    }
}
