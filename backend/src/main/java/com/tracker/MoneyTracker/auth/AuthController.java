package com.tracker.MoneyTracker.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRegistrationService registrationService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public AuthController(UserRegistrationService registrationService,
                          AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UserService userService) {
        this.registrationService = registrationService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody AppUser appUser) {
        if (appUser.getUuid() == null || appUser.getUuid().isBlank()) {
            appUser.setUuid(UUID.randomUUID().toString());
        }
        registrationService.addUser(appUser);
        return ResponseEntity.ok("User registered");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AppUser appUser) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword())
        );
        String token = jwtUtil.generateToken(appUser.getUsername());
        AppUser user = userService.findByUsernameOrThrow(appUser.getUsername());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "userId", user.getUuid(),
                "username", user.getUsername(),
                "email", user.getEmail()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        AppUser user = userService.findByUsernameOrThrow(authentication.getName());
        return ResponseEntity.ok(Map.of(
                "userId", user.getUuid(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        ));
    }

    @DeleteMapping("/users/{username}")
    public ResponseEntity<String> deleteUser(@PathVariable String username) {
        registrationService.deleteUser(username);
        return ResponseEntity.ok("User deleted");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Logged out");
    }

    /**
     * Unauthenticated ping — returns 200 as long as the app is running.
     * Used by Docker health checks.
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    /**
     * Authenticated health-check endpoint.
     * Returns 200 with user info if the JWT token is valid.
     * Frontend uses this to verify backend connectivity + auth status.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "unauthenticated"));
        }
        AppUser user = userService.findByUsernameOrThrow(authentication.getName());
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "userId", user.getUuid(),
                "username", user.getUsername()
        ));
    }
}
