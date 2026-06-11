package com.tracker.MoneyTracker.auth;

import jakarta.persistence.EntityExistsException;
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

    public void deleteUser(AppUser appUser) {
        userRepository.delete(appUser);
    }

    public boolean validateUser(AppUser appUser) throws EntityExistsException {
        if( userRepository.findByUsername(appUser.getUsername()).isPresent() ) {
            throw new EntityExistsException("userAlreadyExists with username " + appUser.getUsername());
        }
        if( userRepository.findByEmail(appUser.getEmail()).isPresent() ) {
            throw new EntityExistsException("userAlreadyExists with email " + appUser.getEmail());
        }
        return true;
    }
}
