package com.tracker.MoneyTracker.exception;

import com.tracker.MoneyTracker.error.ErrorCode;

/**
 * Base exception for all custom application exceptions.
 * Carries an {@link ErrorCode} and optional message arguments for templated messages.
 */
public abstract class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;

    protected BaseException(ErrorCode errorCode, Object... args) {
        super(errorCode.formatMessage(args));
        this.errorCode = errorCode;
        this.args = args;
    }

    protected BaseException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode.formatMessage(args), cause);
        this.errorCode = errorCode;
        this.args = args;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}
