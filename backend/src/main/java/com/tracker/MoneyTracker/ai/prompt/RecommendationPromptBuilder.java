package com.tracker.MoneyTracker.ai.prompt;

import com.tracker.MoneyTracker.analytics.dto.CategoryBreakdown;
import com.tracker.MoneyTracker.analytics.dto.MonthlyTrend;
import com.tracker.MoneyTracker.analytics.dto.SpendingSummary;

import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Builds prompts for the personalized recommendations feature.
 * <p>
 * Constructs a structured prompt from spending summary, category breakdown,
 * and monthly trends to generate actionable financial advice.
 */
@Component
public class RecommendationPromptBuilder {

    /**
     * Builds the full recommendation prompt from the user's financial data.
     *
     * @param summary    the user's spending summary (may be null if unavailable)
     * @param breakdown  category breakdown for recent months
     * @param trends     monthly spending trends
     * @return the complete prompt for the LLM
     */
    public String buildPrompt(SpendingSummary summary,
                              List<CategoryBreakdown> breakdown,
                              List<MonthlyTrend> trends) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a personal financial coach. Based on the following financial data, ");
        prompt.append("provide 3-5 specific, actionable recommendations to help this user save more money ");
        prompt.append("and improve their financial health. Format each recommendation as a separate line.\n\n");

        if (summary != null) {
            prompt.append("Financial Summary:\n");
            prompt.append("- Total Income: ").append(summary.totalIncome()).append("\n");
            prompt.append("- Total Expenses: ").append(summary.totalExpense()).append("\n");
            prompt.append("- Net Savings: ").append(summary.netSavings()).append("\n");
            prompt.append("- Top Spending Category: ").append(summary.topCategory()).append("\n");
            prompt.append("- Transaction Count: ").append(summary.transactionCount()).append("\n\n");
        }

        if (breakdown != null && !breakdown.isEmpty()) {
            prompt.append("Category Breakdown (last 3 months):\n");
            for (CategoryBreakdown cat : breakdown) {
                prompt.append("- ").append(cat.category()).append(": ")
                        .append(cat.amount()).append(" (").append(String.format("%.1f%%", cat.percentage())).append(")\n");
            }
            prompt.append("\n");
        }

        if (trends != null && !trends.isEmpty()) {
            prompt.append("Monthly Spending Trends (last 6 months):\n");
            for (MonthlyTrend trend : trends) {
                prompt.append("- ").append(trend.month()).append(": Income=")
                        .append(trend.income()).append(", Expense=").append(trend.expense())
                        .append(", Net=").append(trend.net()).append("\n");
            }
        }

        prompt.append("\nProvide your recommendations now:");
        return prompt.toString();
    }
}
