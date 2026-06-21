package com.tracker.MoneyTracker.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Standardized error response body returned by the global exception handler.
 *
 * @param status    HTTP status code
 * @param error     HTTP status reason phrase
 * @param message   Human-readable error message
 * @param timestamp When the error occurred
 * @param path      The request path that caused the error
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        String path
) {
    public ErrorResponse {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
