package com.tracker.MoneyTracker.ai.prompt;

import com.tracker.MoneyTracker.analytics.dto.SpendingSummary;
import com.tracker.MoneyTracker.goal.GoalProgress;

import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Builds prompts for the AI chat feature.
 * <p>
 * Isolates prompt engineering from the chat service's orchestration logic.
 * The system prompt establishes the LLM's persona (Chartered Accountant),
 * injects the user's financial data, and includes recent conversation history.
 */
@Component
public class ChatPromptBuilder {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
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
            """;

    private static final String USER_MESSAGE_TEMPLATE = """
            One of your customers has asked the following question. \
            Please think carefully and reply to them professionally:

            Customer Question: %s
            """;

    /**
     * Builds a financial context summary from spending data and goals.
     *
     * @param summary the user's spending summary
     * @param goals   the user's active goal progress
     * @return formatted financial context string
     */
    public String buildFinancialContext(SpendingSummary summary, List<GoalProgress> goals) {
        StringBuilder context = new StringBuilder();

        if (summary != null) {
            context.append("=== User Financial Summary ===\n");
            context.append("Total Income: ").append(summary.totalIncome()).append("\n");
            context.append("Total Expenses: ").append(summary.totalExpense()).append("\n");
            context.append("Net Savings: ").append(summary.netSavings()).append("\n");
            context.append("Top Spending Category: ").append(summary.topCategory()).append("\n");
            context.append("Transaction Count: ").append(summary.transactionCount()).append("\n");
        } else {
            context.append("No spending data available.\n");
        }

        if (goals != null && !goals.isEmpty()) {
            context.append("\n=== Goal Progress ===\n");
            for (GoalProgress goal : goals) {
                context.append(String.format("Goal %s: %.1f%% used (%s / %s)\n",
                        goal.goalId(), goal.percentageUsed(),
                        goal.spentAmount(), goal.targetAmount()));
            }
        }

        return context.toString();
    }

    /**
     * Formats recent chat history into a conversation string.
     *
     * @param historyLines recent messages in chronological order (role: content)
     * @return formatted history string
     */
    public String formatHistory(List<String> historyLines) {
        if (historyLines == null || historyLines.isEmpty()) {
            return "No previous conversation history.";
        }
        return String.join("\n", historyLines);
    }

    /**
     * Builds the full system prompt with financial context and conversation history.
     *
     * @param financialContext the user's financial summary
     * @param historyFormatted recent conversation history
     * @return the complete system prompt
     */
    public String buildSystemPrompt(String financialContext, String historyFormatted) {
        return SYSTEM_PROMPT_TEMPLATE.formatted(financialContext, historyFormatted);
    }

    /**
     * Wraps the user's question with the CA persona context.
     *
     * @param userMessage the user's raw question
     * @return the wrapped user message
     */
    public String wrapUserMessage(String userMessage) {
        return USER_MESSAGE_TEMPLATE.formatted(userMessage);
    }

    /**
     * Combines the system prompt and wrapped user message into the final prompt.
     *
     * @param systemPrompt the system prompt with context
     * @param wrappedUserMessage the wrapped user question
     * @return the complete prompt ready for the LLM
     */
    public String buildFullPrompt(String systemPrompt, String wrappedUserMessage) {
        return systemPrompt + "\n\n" + wrappedUserMessage + "\nAssistant:";
    }
}
