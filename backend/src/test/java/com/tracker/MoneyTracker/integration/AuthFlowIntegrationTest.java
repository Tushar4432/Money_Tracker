package com.tracker.MoneyTracker.integration;

import com.tracker.MoneyTracker.MoneyTrackerApplication;
import com.tracker.MoneyTracker.auth.AppUser;
import com.tracker.MoneyTracker.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import org.springframework.http.HttpStatus;

/**
 * Auth flow integration tests — uses real services against H2 in-memory database.
 * No mocking. Uses RestTemplate for HTTP calls.
 */
@SpringBootTest(classes = MoneyTrackerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Auth Flow Integration Tests")
class AuthFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    private final RestTemplate restTemplate;

    {
        RestTemplate rt = new RestTemplate();
        rt.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                return false;
            }
        });
        this.restTemplate = rt;
    }
    private String baseUrl;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        baseUrl = "http://localhost:" + port + "/money_tracker";
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should register a new user and persist to database")
        void shouldRegisterNewUser() {
            AppUser user = new AppUser();
            user.setUsername("newuser");
            user.setEmail("newuser@example.com");
            user.setPassword("password123");

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/api/v1/auth/register", user, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo("User registered");

            AppUser saved = userRepository.findByUsername("newuser").orElseThrow();
            assertThat(saved.getEmail()).isEqualTo("newuser@example.com");
            assertThat(saved.getUuid()).isNotBlank();
            assertThat(saved.getPassword()).isNotEqualTo("password123");
        }

        @Test
        @DisplayName("Should generate UUID if not provided")
        void shouldGenerateUuid_IfNotProvided() {
            AppUser user = new AppUser();
            user.setUsername("nouuid");
            user.setEmail("nouuid@example.com");
            user.setPassword("password123");
            restTemplate.postForEntity(baseUrl + "/api/v1/auth/register", user, String.class);

            assertThat(userRepository.findByUsername("nouuid").orElseThrow().getUuid()).isNotBlank();
        }

        @Test
        @DisplayName("Should return 409 when username already exists")
        void shouldReturn409_WhenUsernameExists() {
            registerTestUser("duplicate", "first@example.com", "password123");

            AppUser user2 = new AppUser();
            user2.setUsername("duplicate");
            user2.setEmail("second@example.com");
            user2.setPassword("password456");

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/api/v1/auth/register", user2, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Should return 409 when email already exists")
        void shouldReturn409_WhenEmailExists() {
            registerTestUser("user1", "same@example.com", "password123");

            AppUser user2 = new AppUser();
            user2.setUsername("user2");
            user2.setEmail("same@example.com");
            user2.setPassword("password456");

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/api/v1/auth/register", user2, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should login and return JWT token with valid credentials")
        void shouldLoginAndReturnToken() {
            registerTestUser("loginuser", "login@example.com", "mypassword");

            AppUser loginRequest = new AppUser();
            loginRequest.setUsername("loginuser");
            loginRequest.setPassword("mypassword");

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/api/v1/auth/login", loginRequest, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            String token = (String) response.getBody().get("token");
            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("Should return 401 with invalid password")
        void shouldReturn401_WhenInvalidPassword() {
            registerTestUser("wrongpass", "wrongpass@example.com", "correctpass");

            AppUser loginRequest = new AppUser();
            loginRequest.setUsername("wrongpass");
            loginRequest.setPassword("wrongpassword");

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/api/v1/auth/login", loginRequest, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should return 401 when user does not exist")
        void shouldReturn401_WhenUserNotFound() {
            AppUser loginRequest = new AppUser();
            loginRequest.setUsername("nonexistent");
            loginRequest.setPassword("anypassword");

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/api/v1/auth/login", loginRequest, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Protected endpoint access with JWT")
    class ProtectedEndpointTests {

        private String authToken;

        @BeforeEach
        void setUpToken() {
            registerTestUser("protecteduser", "protected@example.com", "securepass");
            authToken = loginAndGetToken("protecteduser", "securepass");
        }

        @Test
        @DisplayName("Should access protected endpoint with valid JWT token")
        void shouldAccessProtectedEndpoint_WithValidToken() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(authToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/transaction?userId=test-id",
                    HttpMethod.GET, entity, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should return 403 without token")
        void shouldReturn403_WithoutToken() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    baseUrl + "/api/v1/transaction?userId=test-id", String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Should return 403 with invalid token")
        void shouldReturn403_WithInvalidToken() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth("invalid.token.here");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/transaction?userId=test-id",
                    HttpMethod.GET, entity, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Should access multiple protected endpoints with same token")
        void shouldAccessMultipleEndpoints_WithSameToken() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(authToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            assertThat(restTemplate.exchange(baseUrl + "/api/v1/transaction?userId=id",
                    HttpMethod.GET, entity, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(restTemplate.exchange(baseUrl + "/api/v1/goals?userId=id",
                    HttpMethod.GET, entity, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(restTemplate.exchange(baseUrl + "/api/v1/notifications?userId=id",
                    HttpMethod.GET, entity, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(restTemplate.exchange(baseUrl + "/api/v1/analytics/summary?userId=id",
                    HttpMethod.GET, entity, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("Should logout successfully")
        void shouldLogoutSuccessfully() {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/api/v1/auth/logout", null, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo("Logged out");
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/auth/users/{username}")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user and prevent subsequent login")
        void shouldDeleteUser_AndPreventLogin() {
            registerTestUser("todelete", "todelete@example.com", "password123");
            String token = loginAndGetToken("todelete", "password123");
            assertThat(token).isNotBlank();

            restTemplate.delete(baseUrl + "/api/v1/auth/users/todelete");

            assertThat(userRepository.findByUsername("todelete")).isEmpty();

            AppUser loginRequest = new AppUser();
            loginRequest.setUsername("todelete");
            loginRequest.setPassword("password123");

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/api/v1/auth/login", loginRequest, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Full auth lifecycle")
    class FullLifecycleTests {

        @Test
        @DisplayName("Should complete full lifecycle: register → login → access → logout")
        void shouldCompleteFullLifecycle() {
            AppUser user = new AppUser();
            user.setUsername("lifecycle");
            user.setEmail("lifecycle@example.com");
            user.setPassword("lifecyclepass");
            assertThat(restTemplate.postForEntity(baseUrl + "/api/v1/auth/register",
                    user, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);

            String token = loginAndGetToken("lifecycle", "lifecyclepass");
            assertThat(token).isNotBlank();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            assertThat(restTemplate.exchange(baseUrl + "/api/v1/transaction?userId=id",
                    HttpMethod.GET, entity, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(restTemplate.postForEntity(baseUrl + "/api/v1/auth/logout",
                    null, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);

            // Token still works (stateless JWT)
            assertThat(restTemplate.exchange(baseUrl + "/api/v1/transaction?userId=id",
                    HttpMethod.GET, entity, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should register multiple users and each can login independently")
        void shouldSupportMultipleUsers() {
            registerTestUser("userA", "a@example.com", "passA");
            registerTestUser("userB", "b@example.com", "passB");

            String tokenA = loginAndGetToken("userA", "passA");
            String tokenB = loginAndGetToken("userB", "passB");

            assertThat(tokenA).isNotBlank();
            assertThat(tokenB).isNotBlank();
            assertThat(tokenA).isNotEqualTo(tokenB);

            HttpHeaders headersA = new HttpHeaders();
            headersA.setBearerAuth(tokenA);
            HttpEntity<Void> entityA = new HttpEntity<>(headersA);

            HttpHeaders headersB = new HttpHeaders();
            headersB.setBearerAuth(tokenB);
            HttpEntity<Void> entityB = new HttpEntity<>(headersB);

            assertThat(restTemplate.exchange(baseUrl + "/api/v1/transaction?userId=id",
                    HttpMethod.GET, entityA, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(restTemplate.exchange(baseUrl + "/api/v1/transaction?userId=id",
                    HttpMethod.GET, entityB, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    private void registerTestUser(String username, String email, String password) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        restTemplate.postForEntity(baseUrl + "/api/v1/auth/register", user, String.class);
    }

    private String loginAndGetToken(String username, String password) {
        AppUser loginRequest = new AppUser();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/auth/login", loginRequest, Map.class);
        return (String) response.getBody().get("token");
    }
}
