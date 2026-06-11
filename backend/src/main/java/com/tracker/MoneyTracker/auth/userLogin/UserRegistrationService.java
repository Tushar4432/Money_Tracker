package com.tracker.MoneyTracker.auth.userLogin;

import com.tracker.MoneyTracker.auth.Constants;
import com.tracker.MoneyTracker.auth.AppUser;
import com.tracker.MoneyTracker.auth.UserRepository;
import com.tracker.MoneyTracker.auth.UserService;
import jakarta.persistence.EntityExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserRegistrationService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(UserService userService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void addUser(AppUser appUser) {
        try{
            userService.validateUser(appUser);
            appUser.setPassword(passwordEncoder.encode(appUser.getPassword()));
            userRepository.save(appUser);
        }catch (Exception e){
            log.error(Constants.UNABLE_TO_REGISTER_USER);
            throw new RuntimeException(e);
        }
    }

    public void deleteUser(String username) {
        AppUser user = userService.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }
        userRepository.delete(user);
    }
}
