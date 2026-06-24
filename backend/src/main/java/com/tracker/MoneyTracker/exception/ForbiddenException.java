package com.tracker.MoneyTracker.exception;

import com.tracker.MoneyTracker.error.ErrorCode;

/**
 * Thrown when the user does not have permission to access a resource.
 * Results in HTTP 403 Forbidden.
 */
public class ForbiddenException extends BaseException {

    public ForbiddenException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
