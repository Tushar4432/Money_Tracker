package com.tracker.MoneyTracker.exception;

import com.tracker.MoneyTracker.error.ErrorCode;

/**
 * Thrown when the request is invalid (missing fields, bad input, unsupported file type, etc.).
 * Results in HTTP 400 Bad Request.
 */
public class BadRequestException extends BaseException {

    public BadRequestException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public BadRequestException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }
}
