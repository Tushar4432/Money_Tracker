package com.tracker.MoneyTracker.ai.service;

import com.tracker.MoneyTracker.ai.client.AiLlmClient;
import com.tracker.MoneyTracker.ai.dto.ChatResponse;
import com.tracker.MoneyTracker.ai.entity.AiChatMessage;
import com.tracker.MoneyTracker.ai.repository.AiChatMessageRepository;
import com.tracker.MoneyTracker.analytics.AnalyticsService;
import com.tracker.MoneyTracker.analytics.dto.SpendingSummary;
import com.tracker.MoneyTracker.goal.GoalService;
import com.tracker.MoneyTracker.goal.GoalProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Core AI chat service.
 * <p>
 * Assembles a rich prompt from the user's financial data (spending summary,
 * goals, recent chat history), sends it to the local Ollama LLM, and persists
 * both the user's message and the AI's response.
 */
@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);
    private static final int MAX_CONTEXT_MESSAGES = 10;

    private final AiLlmClient llmClient;
    private final AiChatMessageRepository chatRepository;
    private final AnalyticsService analyticsService;
    private final GoalService goalService;
    private final Executor aiExecutor;

    public AiChatService(AiLlmClient llmClient,
                         AiChatMessageRepository chatRepository,
                         AnalyticsService analyticsService,
                         GoalService goalService,
                         @Qualifier("aiExecutor") Executor aiExecutor) {
        this.llmClient = llmClient;
        this.chatRepository = chatRepository;
        this.analyticsService = analyticsService;
        this.goalService = goalService;
        this.aiExecutor = aiExecutor;
    }

    /**
     * Processes a user's chat message and returns the AI's response.
     * <p>
     * The method:
     * 1. Fetches the user's spending summary and goals for context
     * 2. Retrieves recent chat history for continuity
     * 3. Builds a system prompt with financial context
     * 4. Sends the prompt to Ollama
     * 5. Persists both user message and AI response
     *
     * @param userId  the authenticated user's ID
     * @param message the user's question
     * @return the AI's response wrapped in a ChatResponse
     */
    public ChatResponse chat(String userId, String message) {
        // Build context-aware prompt — fetch financial data and chat history in parallel
        CompletableFuture<String> contextFuture = CompletableFuture.supplyAsync(
                () -> buildFinancialContext(userId), aiExecutor);
        CompletableFuture<String> historyFuture = CompletableFuture.supplyAsync(
                () -> buildChatHistory(userId), aiExecutor);

        String context = contextFuture.join();
        String history = historyFuture.join();
        String systemPrompt = buildSystemPrompt(context, history);

        // Wrap the user's message with the CA persona context
        String wrappedMessage = """
                One of your customers has asked the following question. \
                Please think carefully and reply to them professionally:

                Customer Question: %s
                """.formatted(message);

        // Combine system prompt with wrapped user message
        String fullPrompt = systemPrompt + "\n\n" + wrappedMessage + "\nAssistant:";

        // Call LLM (provider selected by AiLlmConfig: Ollama / Groq / x.ai / OpenRouter)
        String reply = llmClient.generate(fullPrompt);
        log.info("AI response for user {}: {}", userId, reply.substring(0, Math.min(100, reply.length())));

        // Persist user message
        saveMessage(userId, "USER", message);
        // Persist AI response
        saveMessage(userId, "ASSISTANT", reply);

        return new ChatResponse(userId, message, reply, LocalDateTime.now());
    }

    /**
     * Builds a text summary of the user's financial situation for the LLM prompt.
     */
    private String buildFinancialContext(String userId) {
        StringBuilder context = new StringBuilder();

        try {
            SpendingSummary summary = analyticsService.getSpendingSummary(userId);
            context.append("=== User Financial Summary ===\n");
            context.append("Total Income: ").append(summary.totalIncome()).append("\n");
            context.append("Total Expenses: ").append(summary.totalExpense()).append("\n");
            context.append("Net Savings: ").append(summary.netSavings()).append("\n");
            context.append("Top Spending Category: ").append(summary.topCategory()).append("\n");
            context.append("Transaction Count: ").append(summary.transactionCount()).append("\n");
        } catch (Exception e) {
            log.warn("Could not fetch spending summary for user {}", userId, e);
            context.append("No spending data available.\n");
        }

        try {
            List<GoalProgress> goals = goalService.evaluateAllGoals(userId);
            if (!goals.isEmpty()) {
                context.append("\n=== Goal Progress ===\n");
                for (GoalProgress goal : goals) {
                    context.append(String.format("Goal %s: %.1f%% used (%s / %s)\n",
                            goal.goalId(), goal.percentageUsed(),
                            goal.spentAmount(), goal.targetAmount()));
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch goal progress for user {}", userId, e);
        }

        return context.toString();
    }

    /**
     * Retrieves recent chat history to provide conversation continuity.
     */
    private String buildChatHistory(String userId) {
        try {
            List<AiChatMessage> recentMessages = chatRepository
                    .findTop20ByUserIdOrderByCreatedAtDesc(userId);

            if (recentMessages.isEmpty()) {
                return "No previous conversation history.";
            }

            // Reverse to chronological order
            java.util.Collections.reverse(recentMessages);

            // Limit to last N messages for context window
            int startIdx = Math.max(0, recentMessages.size() - MAX_CONTEXT_MESSAGES);
            List<AiChatMessage> contextMessages = recentMessages.subList(startIdx, recentMessages.size());

            return contextMessages.stream()
                    .map(m -> m.getRole() + ": " + m.getContent())
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.warn("Could not fetch chat history for user {}", userId, e);
            return "No previous conversation history.";
        }
    }

    /**
     * Builds the system prompt that instructs the LLM on its role and context.
     */
    private String buildSystemPrompt(String context, String history) {
        return """
                You are a Chartered Accountant with decades of experience. You have started advising \
                people who want you to manage their spending. You have access to their spending \
                analytics. Based on the data, you advise based on what they ask of you. \
                You may get requests to come up with a plan to save money, create budgets, \
                analyze spending patterns, and provide actionable financial advice.

                You have access to the user's financial data and goals. Be specific, actionable, \
                and supportive in your advice. Use your professional expertise as a CA to provide \
                authoritative guidance.

                Guidelines:
                - Reference specific numbers from the user's data when giving advice
                - Suggest concrete actions the user can take
                - If the user mentions a savings goal, help them create or track it
                - Be encouraging but honest about financial realities
                - Provide structured advice (use bullet points or numbered lists when helpful)
                - Keep responses concise and professional (2-4 paragraphs max)
                - When discussing budgets, always ground recommendations in the user's actual income and expenses

                User Financial Data:
                %s

                Recent Conversation:
                %s
                """.formatted(context, history);
    }

    private void saveMessage(String userId, String role, String content) {
        AiChatMessage entity = new AiChatMessage();
        entity.setUserId(userId);
        entity.setRole(role);
        entity.setContent(content);
        chatRepository.save(entity);
    }
}
