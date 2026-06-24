package com.tracker.MoneyTracker.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for the AI chat endpoint.
 *
 * @param userId  the authenticated user's ID
 * @param message the user's question or message
 */
public record ChatRequest(
        @NotNull(message = "userId is required") String userId,
        @NotBlank(message = "message must not be blank") String message
) {
}
