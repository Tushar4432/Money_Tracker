package com.tracker.MoneyTracker.auth;

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

    public AuthController(UserRegistrationService registrationService,
                          AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil) {
        this.registrationService = registrationService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
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
        return ResponseEntity.ok(Map.of("token", token));
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
}
