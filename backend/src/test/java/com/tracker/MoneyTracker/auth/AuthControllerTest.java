package com.tracker.MoneyTracker.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracker.MoneyTracker.error.GlobalExceptionHandler;
import com.tracker.MoneyTracker.exception.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Mock
    private UserRegistrationService registrationService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(registrationService, authenticationManager, jwtUtil);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    private AppUser createUser(String username, String email, String password) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        return user;
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user when valid input provided")
        void shouldRegisterNewUser_WhenValidInput() throws Exception {
            // Arrange
            AppUser user = createUser("john", "john@example.com", "password123");
            willDoNothing().given(registrationService).addUser(any(AppUser.class));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("User registered"));
        }

        @Test
        @DisplayName("Should return bad request when username already exists")
        void shouldReturnBadRequest_WhenUsernameExists() throws Exception {
            // Arrange
            AppUser user = createUser("existing", "new@example.com", "password123");
            willThrow(new DuplicateResourceException(
                    com.tracker.MoneyTracker.error.ErrorCode.USER_ALREADY_EXISTS, "test"))
                    .given(registrationService).addUser(any(AppUser.class));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return bad request when email already exists")
        void shouldReturnBadRequest_WhenEmailExists() throws Exception {
            // Arrange
            AppUser user = createUser("newuser", "existing@example.com", "password123");
            willThrow(new DuplicateResourceException(
                    com.tracker.MoneyTracker.error.ErrorCode.USER_ALREADY_EXISTS, "test"))
                    .given(registrationService).addUser(any(AppUser.class));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should generate UUID for new user during registration")
        void shouldGenerateUUID_ForNewUser() throws Exception {
            // Arrange
            AppUser user = createUser("newuser", "new@example.com", "password123");
            willDoNothing().given(registrationService).addUser(any(AppUser.class));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isOk());

            then(registrationService).should().addUser(argThat(u ->
                    u.getUuid() != null && !u.getUuid().isBlank()
            ));
        }
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("Should login and return JWT token when credentials are valid")
        void shouldLoginAndReturnToken_WhenValidCredentials() throws Exception {
            // Arrange
            AppUser user = createUser("john", "john@example.com", "password123");
            Authentication auth = new UsernamePasswordAuthenticationToken("john", null, java.util.List.of());
            given(authenticationManager.authenticate(any())).willReturn(auth);
            given(jwtUtil.generateToken("john")).willReturn("mock.jwt.token");

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("mock.jwt.token"));
        }

        @Test
        @DisplayName("Should return unauthorized when credentials are invalid")
        void shouldReturnUnauthorized_WhenInvalidCredentials() throws Exception {
            // Arrange
            AppUser user = createUser("john", "john@example.com", "wrongpassword");
            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("Bad credentials"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return unauthorized when user not found")
        void shouldReturnUnauthorized_WhenUserNotFound() throws Exception {
            // Arrange
            AppUser user = createUser("unknown", "unknown@example.com", "password123");
            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("User not found"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("Should logout successfully when authenticated")
        void shouldLogoutSuccessfully_WhenAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Logged out"));
        }
    }
}
