package com.tracker.MoneyTracker.error;

import com.tracker.MoneyTracker.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;

/**
 * Global exception handler that catches all exceptions thrown by controllers
 * and returns a standardized {@link ErrorResponse} body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        return buildResponse(code, code.formatMessage(ex.getArgs()), request, code.getHttpStatus());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        return buildResponse(code, code.formatMessage(ex.getArgs()), request, code.getHttpStatus());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        return buildResponse(code, code.formatMessage(ex.getArgs()), request, code.getHttpStatus());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        return buildResponse(code, code.formatMessage(ex.getArgs()), request, code.getHttpStatus());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        return buildResponse(code, code.formatMessage(ex.getArgs()), request, code.getHttpStatus());
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<ErrorResponse> handleInternalServer(
            InternalServerException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        return buildResponse(code, code.formatMessage(ex.getArgs()), request, code.getHttpStatus());
    }

    // --- Spring Security exceptions ---

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return buildResponse(ErrorCode.INVALID_CREDENTIALS, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(ErrorCode.ACCESS_DENIED, request);
    }

    // --- Spring MVC exceptions ---

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return buildResponse(ErrorCode.METHOD_NOT_ALLOWED, request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {
        return buildResponse(ErrorCode.ENDPOINT_NOT_FOUND, request);
    }

    // --- Fallback for IllegalArgumentException (backward compatibility) ---

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildResponse(ErrorCode.INVALID_INPUT, ex.getMessage(), request, HttpStatus.BAD_REQUEST);
    }

    // --- Catch-all for any unhandled exception ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        return buildResponse(ErrorCode.INTERNAL_ERROR, request);
    }

    // --- Helpers ---

    private ResponseEntity<ErrorResponse> buildResponse(ErrorCode code, HttpServletRequest request) {
        return buildResponse(code, code.getMessageTemplate(), request, code.getHttpStatus());
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            ErrorCode code, String message, HttpServletRequest request, HttpStatus status) {
        ErrorResponse body = new ErrorResponse(
                status.value(),
                code.getError(),
                message,
                LocalDateTime.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}
