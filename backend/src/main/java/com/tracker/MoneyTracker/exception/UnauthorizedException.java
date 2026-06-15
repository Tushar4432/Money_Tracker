package com.tracker.MoneyTracker.exception;

import com.tracker.MoneyTracker.error.ErrorCode;

/**
 * Thrown when authentication fails (bad credentials, expired/invalid token).
 * Results in HTTP 401 Unauthorized.
 */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
