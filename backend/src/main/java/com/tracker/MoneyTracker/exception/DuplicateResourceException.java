package com.tracker.MoneyTracker.exception;

import com.tracker.MoneyTracker.error.ErrorCode;

/**
 * Thrown when a resource already exists (duplicate username, email, etc.).
 * Results in HTTP 409 Conflict.
 */
public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
