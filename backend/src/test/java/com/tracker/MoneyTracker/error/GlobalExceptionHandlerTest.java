package com.tracker.MoneyTracker.error;

import com.tracker.MoneyTracker.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler sut;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        sut = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");
    }

    @Nested
    @DisplayName("ResourceNotFoundException -> 404")
    class ResourceNotFoundTests {

        @Test
        @DisplayName("Should return 404 with error body")
        void shouldReturn404WithErrorBody() {
            ResourceNotFoundException ex = new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "john");

            ResponseEntity<ErrorResponse> response = sut.handleResourceNotFound(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(404);
            assertThat(response.getBody().error()).isEqualTo("Not Found");
            assertThat(response.getBody().message()).isEqualTo("User not found: john");
            assertThat(response.getBody().path()).isEqualTo("/api/v1/test");
            assertThat(response.getBody().timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("DuplicateResourceException -> 409")
    class DuplicateResourceTests {

        @Test
        @DisplayName("Should return 409 with error body")
        void shouldReturn409WithErrorBody() {
            DuplicateResourceException ex = new DuplicateResourceException(ErrorCode.USER_ALREADY_EXISTS, "john");

            ResponseEntity<ErrorResponse> response = sut.handleDuplicateResource(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(409);
            assertThat(response.getBody().message()).isEqualTo("User already exists: john");
        }
    }

    @Nested
    @DisplayName("BadRequestException -> 400")
    class BadRequestTests {

        @Test
        @DisplayName("Should return 400 with error body")
        void shouldReturn400WithErrorBody() {
            BadRequestException ex = new BadRequestException(ErrorCode.UNSUPPORTED_FILE_TYPE);

            ResponseEntity<ErrorResponse> response = sut.handleBadRequest(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().message()).isEqualTo("Unsupported file type");
        }
    }

    @Nested
    @DisplayName("UnauthorizedException -> 401")
    class UnauthorizedTests {

        @Test
        @DisplayName("Should return 401 with error body")
        void shouldReturn401WithErrorBody() {
            UnauthorizedException ex = new UnauthorizedException(ErrorCode.TOKEN_EXPIRED);

            ResponseEntity<ErrorResponse> response = sut.handleUnauthorized(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(401);
            assertThat(response.getBody().message()).isEqualTo("Token has expired");
        }
    }

    @Nested
    @DisplayName("ForbiddenException -> 403")
    class ForbiddenTests {

        @Test
        @DisplayName("Should return 403 with error body")
        void shouldReturn403WithErrorBody() {
            ForbiddenException ex = new ForbiddenException(ErrorCode.ACCESS_DENIED);

            ResponseEntity<ErrorResponse> response = sut.handleForbidden(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("InternalServerException -> 500")
    class InternalServerTests {

        @Test
        @DisplayName("Should return 500 with error body")
        void shouldReturn500WithErrorBody() {
            InternalServerException ex = new InternalServerException(ErrorCode.INTERNAL_ERROR, new RuntimeException("DB error"));

            ResponseEntity<ErrorResponse> response = sut.handleInternalServer(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("BadCredentialsException (Spring Security) -> 401")
    class BadCredentialsTests {

        @Test
        @DisplayName("Should return 401 for bad credentials")
        void shouldReturn401ForBadCredentials() {
            BadCredentialsException ex = new BadCredentialsException("Bad credentials");

            ResponseEntity<ErrorResponse> response = sut.handleBadCredentials(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(401);
            assertThat(response.getBody().message()).isEqualTo("Invalid credentials");
        }
    }

    @Nested
    @DisplayName("AccessDeniedException (Spring Security) -> 403")
    class AccessDeniedTests {

        @Test
        @DisplayName("Should return 403 for access denied")
        void shouldReturn403ForAccessDenied() {
            AccessDeniedException ex = new AccessDeniedException("Access denied");

            ResponseEntity<ErrorResponse> response = sut.handleAccessDenied(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("IllegalArgumentException (fallback) -> 400")
    class IllegalArgumentTests {

        @Test
        @DisplayName("Should return 400 for illegal argument")
        void shouldReturn400ForIllegalArgument() {
            IllegalArgumentException ex = new IllegalArgumentException("some field is invalid");

            ResponseEntity<ErrorResponse> response = sut.handleIllegalArgument(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().message()).isEqualTo("some field is invalid");
        }
    }

    @Nested
    @DisplayName("Generic Exception (catch-all) -> 500")
    class GenericExceptionTests {

        @Test
        @DisplayName("Should return 500 for unhandled exceptions")
        void shouldReturn500ForUnhandled() {
            Exception ex = new RuntimeException("unexpected error");

            ResponseEntity<ErrorResponse> response = sut.handleGeneric(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(500);
            assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        }
    }
}
