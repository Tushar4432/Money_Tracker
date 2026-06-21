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

import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Generates personalized financial recommendations by analyzing spending patterns
 * and feeding them to the local LLM.
 * <p>
 * Data fetching is parallelized: spending summary, category breakdown, and monthly
 * trends are independent queries dispatched concurrently before a single LLM call.
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final OllamaClient ollamaClient;
    private final AnalyticsService analyticsService;
    private final Executor aiExecutor;

    public RecommendationService(OllamaClient ollamaClient,
                                 AnalyticsService analyticsService,
                                 @Qualifier("aiExecutor") Executor aiExecutor) {
        this.ollamaClient = ollamaClient;
        this.analyticsService = analyticsService;
        this.aiExecutor = aiExecutor;
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
        // Fetch all three data sources in parallel — they are independent DB queries
        CompletableFuture<SpendingSummary> summaryFuture = CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return analyticsService.getSpendingSummary(userId);
                    } catch (Exception e) {
                        log.warn("Could not fetch spending summary for recommendations", e);
                        return null;
                    }
                }, aiExecutor);

        CompletableFuture<List<CategoryBreakdown>> breakdownFuture = CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return analyticsService.getCategoryBreakdown(
                                userId,
                                LocalDate.now().minusMonths(3),
                                LocalDate.now());
                    } catch (Exception e) {
                        log.warn("Could not fetch category breakdown for recommendations", e);
                        return List.<CategoryBreakdown>of();
                    }
                }, aiExecutor);

        CompletableFuture<List<MonthlyTrend>> trendsFuture = CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return analyticsService.getMonthlyTrends(userId, 6);
                    } catch (Exception e) {
                        log.warn("Could not fetch monthly trends for recommendations", e);
                        return List.<MonthlyTrend>of();
                    }
                }, aiExecutor);

        // Await all three results
        SpendingSummary summary = summaryFuture.join();
        List<CategoryBreakdown> breakdown = breakdownFuture.join();
        List<MonthlyTrend> trends = trendsFuture.join();

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

        if (!breakdown.isEmpty()) {
            prompt.append("Category Breakdown (last 3 months):\n");
            for (CategoryBreakdown cat : breakdown) {
                prompt.append("- ").append(cat.category()).append(": ")
                        .append(cat.amount()).append(" (").append(String.format("%.1f%%", cat.percentage())).append(")\n");
            }
            prompt.append("\n");
        }

        if (!trends.isEmpty()) {
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
