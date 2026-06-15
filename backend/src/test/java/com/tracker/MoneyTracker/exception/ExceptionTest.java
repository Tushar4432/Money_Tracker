package com.tracker.MoneyTracker.exception;

import com.tracker.MoneyTracker.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Custom Exceptions")
class ExceptionTest {

    @Nested
    @DisplayName("ResourceNotFoundException")
    class ResourceNotFoundExceptionTests {

        @Test
        @DisplayName("Should carry correct ErrorCode and message")
        void shouldCarryCorrectErrorCodeAndMessage() {
            ResourceNotFoundException ex = new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "john");

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
            assertThat(ex.getMessage()).isEqualTo("User not found: john");
            assertThat(ex.getArgs()).containsExactly("john");
        }

        @Test
        @DisplayName("Should map to HTTP 404")
        void shouldMapTo404() {
            ResourceNotFoundException ex = new ResourceNotFoundException(ErrorCode.GOAL_NOT_FOUND, "goal-123");

            assertThat(ex.getErrorCode().getHttpStatus().value()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("DuplicateResourceException")
    class DuplicateResourceExceptionTests {

        @Test
        @DisplayName("Should carry correct ErrorCode and message")
        void shouldCarryCorrectErrorCodeAndMessage() {
            DuplicateResourceException ex = new DuplicateResourceException(ErrorCode.USER_ALREADY_EXISTS, "john");

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
            assertThat(ex.getMessage()).isEqualTo("User already exists: john");
        }

        @Test
        @DisplayName("Should map to HTTP 409")
        void shouldMapTo409() {
            DuplicateResourceException ex = new DuplicateResourceException(ErrorCode.USER_ALREADY_EXISTS, "john");

            assertThat(ex.getErrorCode().getHttpStatus().value()).isEqualTo(409);
        }
    }

    @Nested
    @DisplayName("BadRequestException")
    class BadRequestExceptionTests {

        @Test
        @DisplayName("Should carry correct ErrorCode and message")
        void shouldCarryCorrectErrorCodeAndMessage() {
            BadRequestException ex = new BadRequestException(ErrorCode.INVALID_INPUT, "bad data");

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
            assertThat(ex.getMessage()).isEqualTo("Invalid input: bad data");
        }

        @Test
        @DisplayName("Should map to HTTP 400")
        void shouldMapTo400() {
            BadRequestException ex = new BadRequestException(ErrorCode.UNSUPPORTED_FILE_TYPE);

            assertThat(ex.getErrorCode().getHttpStatus().value()).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("UnauthorizedException")
    class UnauthorizedExceptionTests {

        @Test
        @DisplayName("Should carry correct ErrorCode and message")
        void shouldCarryCorrectErrorCodeAndMessage() {
            UnauthorizedException ex = new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
            assertThat(ex.getMessage()).isEqualTo("Invalid credentials");
        }

        @Test
        @DisplayName("Should map to HTTP 401")
        void shouldMapTo401() {
            UnauthorizedException ex = new UnauthorizedException(ErrorCode.TOKEN_EXPIRED);

            assertThat(ex.getErrorCode().getHttpStatus().value()).isEqualTo(401);
        }
    }

    @Nested
    @DisplayName("ForbiddenException")
    class ForbiddenExceptionTests {

        @Test
        @DisplayName("Should carry correct ErrorCode")
        void shouldCarryCorrectErrorCode() {
            ForbiddenException ex = new ForbiddenException(ErrorCode.ACCESS_DENIED);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        }

        @Test
        @DisplayName("Should map to HTTP 403")
        void shouldMapTo403() {
            ForbiddenException ex = new ForbiddenException(ErrorCode.ACCESS_DENIED);

            assertThat(ex.getErrorCode().getHttpStatus().value()).isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("InternalServerException")
    class InternalServerExceptionTests {

        @Test
        @DisplayName("Should carry correct ErrorCode")
        void shouldCarryCorrectErrorCode() {
            RuntimeException cause = new RuntimeException("DB error");
            InternalServerException ex = new InternalServerException(ErrorCode.INTERNAL_ERROR, cause);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
            assertThat(ex.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("Should map to HTTP 500")
        void shouldMapTo500() {
            InternalServerException ex = new InternalServerException(ErrorCode.INTERNAL_ERROR, new RuntimeException("oops"));

            assertThat(ex.getErrorCode().getHttpStatus().value()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("ErrorCode formatMessage")
    class ErrorCodeFormatMessageTests {

        @Test
        @DisplayName("Should format message with single argument")
        void shouldFormatWithSingleArg() {
            String msg = ErrorCode.USER_NOT_FOUND.formatMessage("john");
            assertThat(msg).isEqualTo("User not found: john");
        }

        @Test
        @DisplayName("Should format message with multiple arguments")
        void shouldFormatWithMultipleArgs() {
            String msg = ErrorCode.USER_ALREADY_EXISTS.formatMessage("john");
            assertThat(msg).isEqualTo("User already exists: john");
        }

        @Test
        @DisplayName("Should return template as-is when no args provided")
        void shouldReturnTemplateWhenNoArgs() {
            String msg = ErrorCode.UNSUPPORTED_FILE_TYPE.formatMessage();
            assertThat(msg).isEqualTo("Unsupported file type");
        }

        @Test
        @DisplayName("Should return template as-is when args are null")
        void shouldReturnTemplateWhenArgsNull() {
            String msg = ErrorCode.INTERNAL_ERROR.formatMessage((Object[]) null);
            assertThat(msg).isEqualTo("An unexpected error occurred");
        }
    }
}
