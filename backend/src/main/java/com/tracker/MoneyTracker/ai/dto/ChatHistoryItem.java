package com.tracker.MoneyTracker.ai.dto;

import java.time.LocalDateTime;

/**
 * A single message in the chat history, returned by the API.
 * <p>
 * Exposes only the fields the client needs — never the JPA entity directly.
 */
public record ChatHistoryItem(
        String role,
        String content,
        LocalDateTime createdAt
) {
}
