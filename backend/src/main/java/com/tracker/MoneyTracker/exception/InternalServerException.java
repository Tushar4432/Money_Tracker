package com.tracker.MoneyTracker.exception;

import com.tracker.MoneyTracker.error.ErrorCode;

/**
 * Thrown when an unexpected internal error occurs.
 * Results in HTTP 500 Internal Server Error.
 */
public class InternalServerException extends BaseException {

    public InternalServerException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public InternalServerException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }
}
