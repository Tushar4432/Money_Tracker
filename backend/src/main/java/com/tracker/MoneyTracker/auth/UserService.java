package com.tracker.MoneyTracker.auth;

import com.tracker.MoneyTracker.error.ErrorCode;
import com.tracker.MoneyTracker.exception.DuplicateResourceException;
import com.tracker.MoneyTracker.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AppUser findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public AppUser findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public AppUser findById(String id) {
        return userRepository.findById(id).orElse(null);
    }

    public AppUser findByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, username));
    }

    public void deleteUser(AppUser appUser) {
        userRepository.delete(appUser);
    }

    public void validateUser(AppUser appUser) {
        if (userRepository.findByUsername(appUser.getUsername()).isPresent()) {
            throw new DuplicateResourceException(ErrorCode.USER_ALREADY_EXISTS, appUser.getUsername());
        }
        if (userRepository.findByEmail(appUser.getEmail()).isPresent()) {
            throw new DuplicateResourceException(ErrorCode.USER_ALREADY_EXISTS, appUser.getEmail());
        }
    }
}
