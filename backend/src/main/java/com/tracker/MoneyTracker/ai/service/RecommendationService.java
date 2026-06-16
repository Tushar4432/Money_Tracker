package com.tracker.MoneyTracker.ai.service;

import com.tracker.MoneyTracker.ai.client.OllamaClient;
import com.tracker.MoneyTracker.ai.dto.RecommendationResponse;
import com.tracker.MoneyTracker.analytics.AnalyticsService;
import com.tracker.MoneyTracker.analytics.dto.CategoryBreakdown;
import com.tracker.MoneyTracker.analytics.dto.MonthlyTrend;
import com.tracker.MoneyTracker.analytics.dto.SpendingSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Generates personalized financial recommendations by analyzing spending patterns
 * and feeding them to the local LLM.
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final OllamaClient ollamaClient;
    private final AnalyticsService analyticsService;

    public RecommendationService(OllamaClient ollamaClient, AnalyticsService analyticsService) {
        this.ollamaClient = ollamaClient;
        this.analyticsService = analyticsService;
    }

    /**
     * Generates personalized spending recommendations for a user.
     *
     * @param userId the user ID
     * @return recommendations response with actionable advice
     */
    public RecommendationResponse generateRecommendations(String userId) {
        String prompt = buildRecommendationPrompt(userId);
        String aiResponse = ollamaClient.generate(prompt);

        // Split the AI response into individual recommendations
        List<String> recommendations = parseRecommendations(aiResponse);

        return new RecommendationResponse(userId, recommendations);
    }

    private String buildRecommendationPrompt(String userId) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a personal financial coach. Based on the following financial data, ");
        prompt.append("provide 3-5 specific, actionable recommendations to help this user save more money ");
        prompt.append("and improve their financial health. Format each recommendation as a separate line.\n\n");

        try {
            SpendingSummary summary = analyticsService.getSpendingSummary(userId);
            prompt.append("Financial Summary:\n");
            prompt.append("- Total Income: ").append(summary.totalIncome()).append("\n");
            prompt.append("- Total Expenses: ").append(summary.totalExpense()).append("\n");
            prompt.append("- Net Savings: ").append(summary.netSavings()).append("\n");
            prompt.append("- Top Spending Category: ").append(summary.topCategory()).append("\n");
            prompt.append("- Transaction Count: ").append(summary.transactionCount()).append("\n\n");
        } catch (Exception e) {
            log.warn("Could not fetch spending summary for recommendations", e);
        }

        try {
            List<CategoryBreakdown> breakdown = analyticsService.getCategoryBreakdown(
                    userId,
                    LocalDate.now().minusMonths(3),
                    LocalDate.now()
            );
            if (!breakdown.isEmpty()) {
                prompt.append("Category Breakdown (last 3 months):\n");
                for (CategoryBreakdown cat : breakdown) {
                    prompt.append("- ").append(cat.category()).append(": ")
                            .append(cat.amount()).append(" (").append(String.format("%.1f%%", cat.percentage())).append(")\n");
                }
                prompt.append("\n");
            }
        } catch (Exception e) {
            log.warn("Could not fetch category breakdown for recommendations", e);
        }

        try {
            List<MonthlyTrend> trends = analyticsService.getMonthlyTrends(userId, 6);
            if (!trends.isEmpty()) {
                prompt.append("Monthly Spending Trends (last 6 months):\n");
                for (MonthlyTrend trend : trends) {
                    prompt.append("- ").append(trend.month()).append(": Income=")
                            .append(trend.income()).append(", Expense=").append(trend.expense())
                            .append(", Net=").append(trend.net()).append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch monthly trends for recommendations", e);
        }

        prompt.append("\nProvide your recommendations now:");
        return prompt.toString();
    }

    /**
     * Parses the AI response text into individual recommendation strings.
     * Handles both numbered lists and bullet-point lists.
     */
    private List<String> parseRecommendations(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return List.of("No recommendations available at this time.");
        }

        return aiResponse.lines()
                .map(String::strip)
                .filter(line -> !line.isBlank())
                .map(line -> {
                    // Strip leading numbering like "1.", "2.", "-", "*"
                    if (line.matches("^\\d+\\.\\s+.*")) {
                        return line.replaceFirst("^\\d+\\.\\s+", "");
                    }
                    if (line.matches("^[-*]\\s+.*")) {
                        return line.replaceFirst("^[-*]\\s+", "");
                    }
                    return line;
                })
                .filter(line -> line.length() > 10) // Filter out very short fragments
                .toList();
    }
}
