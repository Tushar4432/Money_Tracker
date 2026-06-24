package com.tracker.MoneyTracker.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtil")
class JwtUtilTest {

    private JwtUtil sut;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() throws Exception {
        sut = new JwtUtil();
        // Inject secret and expiration via reflection since @Value is not available in plain unit tests
        setField(sut, "secret", Base64.getEncoder().encodeToString("testSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm123456".getBytes()));
        setField(sut, "expiration", 86400000L);
        userDetails = User.withUsername("testuser")
                .password("password")
                .roles("USER")
                .build();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateTokenTests {

        @Test
        @DisplayName("Should generate valid token when username provided")
        void shouldGenerateValidToken_WhenUsernameProvided() {
            String token = sut.generateToken(userDetails.getUsername());

            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("Should encode username as subject in token")
        void shouldEncodeUsernameAsSubject() {
            String token = sut.generateToken("john");
            String extracted = sut.extractUsername(token);
            assertThat(extracted).isEqualTo("john");
        }

        @Test
        @DisplayName("Should set expiration in the future")
        void shouldSetExpiration_InFuture() {
            String token = sut.generateToken(userDetails.getUsername());
            Date expiration = sut.extractExpiration(token);
            assertThat(expiration).isAfter(new Date());
        }
    }

    @Nested
    @DisplayName("extractUsername")
    class ExtractUsernameTests {

        @Test
        @DisplayName("Should extract username when token is valid")
        void shouldExtractUsername_WhenTokenIsValid() {
            String token = sut.generateToken("alice");
            String username = sut.extractUsername(token);
            assertThat(username).isEqualTo("alice");
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateTokenTests {

        @Test
        @DisplayName("Should return true when token is valid and matches user")
        void shouldReturnTrue_WhenTokenValidAndMatchesUser() {
            String token = sut.generateToken("testuser");
            boolean valid = sut.validateToken(token, userDetails);
            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("Should return false when username does not match")
        void shouldReturnFalse_WhenUsernameMismatch() {
            String token = sut.generateToken("differentuser");
            boolean valid = sut.validateToken(token, userDetails);
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("Should return false when token is expired")
        void shouldReturnFalse_WhenTokenExpired() {
            // Create an expired token signed with a DIFFERENT key — should fail validation
            SecretKey differentKey = Keys.hmacShaKeyFor(
                    Base64.getEncoder().encodeToString("aDifferentKeyThatIsAlsoLongEnoughForJWT1234567890AB".getBytes()).getBytes());
            String expiredToken = Jwts.builder()
                    .subject("testuser")
                    .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                    .expiration(new Date(System.currentTimeMillis() - 3600000))
                    .signWith(differentKey)
                    .compact();

            boolean valid = sut.validateToken(expiredToken, userDetails);
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("Should return false when token has wrong signature")
        void shouldReturnFalse_WhenWrongSignature() {
            SecretKey wrongKey = Keys.hmacShaKeyFor(
                    Base64.getEncoder().encodeToString("wrongSecretKeyThatIsAtLeast256BitsLongForAlgorithm!!".getBytes()).getBytes());
            String tamperedToken = Jwts.builder()
                    .subject("testuser")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 86400000))
                    .signWith(wrongKey)
                    .compact();

            boolean valid = sut.validateToken(tamperedToken, userDetails);
            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("isTokenExpired")
    class IsTokenExpiredTests {

        @Test
        @DisplayName("Should return false for a fresh token")
        void shouldReturnFalse_ForFreshToken() {
            String token = sut.generateToken(userDetails.getUsername());
            boolean expired = sut.isTokenExpired(token);
            assertThat(expired).isFalse();
        }
    }
}
