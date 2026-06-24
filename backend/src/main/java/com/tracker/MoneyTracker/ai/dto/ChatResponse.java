package com.tracker.MoneyTracker.ai.dto;

import java.time.LocalDateTime;

/**
 * Response from the AI chat endpoint.
 *
 * @param userId     the user who sent the message
 * @param message    the user's original message
 * @param reply     the AI-generated reply
 * @param createdAt  timestamp of the response
 */
public record ChatResponse(
        String userId,
        String message,
        String reply,
        LocalDateTime createdAt
) {
}
