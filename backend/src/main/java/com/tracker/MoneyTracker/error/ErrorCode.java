package com.tracker.MoneyTracker.error;

import org.springframework.http.HttpStatus;

/**
 * Centralized error codes for the application.
 * Each constant defines the HTTP status, error type, and a message template.
 * Use {@code {0}}, {@code {1}}, etc. as placeholders for message arguments.
 */
public enum ErrorCode {

    // --- 400 Bad Request ---
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "Bad Request", "Invalid input: {0}"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Bad Request", "Validation failed: {0}"),
    MISSING_FIELD(HttpStatus.BAD_REQUEST, "Bad Request", "Required field missing: {0}"),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "Bad Request", "Unsupported file type"),
    FILE_PARSE_ERROR(HttpStatus.BAD_REQUEST, "Bad Request", "Failed to parse file"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "Bad Request", "Invalid date range"),

    // --- 401 Unauthorized ---
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid credentials"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Unauthorized", "Token has expired"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid token"),

    // --- 403 Forbidden ---
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Forbidden", "Access denied"),

    // --- 404 Not Found ---
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Not Found", "User not found: {0}"),
    GOAL_NOT_FOUND(HttpStatus.NOT_FOUND, "Not Found", "Goal not found: {0}"),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Not Found", "Notification not found: {0}"),
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "Not Found", "Endpoint not found"),

    // --- 405 Method Not Allowed ---
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", "Method not allowed"),

    // --- 409 Conflict ---
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "Conflict", "User already exists: {0}"),

    // --- 502 Bad Gateway ---
    LLM_ERROR(HttpStatus.BAD_GATEWAY, "Bad Gateway", "AI service unavailable"),

    // --- 500 Internal Server Error ---
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred");

    private final HttpStatus httpStatus;
    private final String error;
    private final String messageTemplate;

    ErrorCode(HttpStatus httpStatus, String error, String messageTemplate) {
        this.httpStatus = httpStatus;
        this.error = error;
        this.messageTemplate = messageTemplate;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getError() {
        return error;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    /**
     * Formats the message template with the given arguments.
     * Replaces {0}, {1}, etc. with provided args.
     */
    public String formatMessage(Object... args) {
        String message = messageTemplate;
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                message = message.replace("{" + i + "}", String.valueOf(args[i]));
            }
        }
        return message;
    }
}
