package com.tracker.MoneyTracker.auth;

import com.tracker.MoneyTracker.error.ErrorCode;
import com.tracker.MoneyTracker.exception.DuplicateResourceException;
import com.tracker.MoneyTracker.exception.InternalServerException;
import com.tracker.MoneyTracker.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserRegistrationService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(UserService userService, UserRepository userRepository,
                                   PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void addUser(AppUser appUser) {
        try {
            userService.validateUser(appUser);
            appUser.setPassword(passwordEncoder.encode(appUser.getPassword()));
            userRepository.save(appUser);
        } catch (DuplicateResourceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unable to register user: {}", e.getMessage(), e);
            throw new InternalServerException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    public void deleteUser(String username) {
        AppUser user = userService.findByUsernameOrThrow(username);
        userRepository.delete(user);
    }
}
