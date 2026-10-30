package com.ztna.service;

import com.ztna.model.User;
import com.ztna.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Handles user registration and credential verification.
 * IP tracking is fully delegated to NetworkService — this class has no
 * knowledge of the two-IP hard limit that existed in the old UserService.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;
    private final SecurityLogService securityLogService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       LoginAttemptService loginAttemptService,
                       SecurityLogService securityLogService) {
        this.userRepository = userRepository;
        this.loginAttemptService = loginAttemptService;
        this.securityLogService = securityLogService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // ── Registration ─────────────────────────────────────────────────────────

    public User registerUser(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER"); // All registrations create standard USER accounts
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);

        securityLogService.logEvent(
                username,
                "REGISTER_SUCCESS",
                "SYSTEM",
                "New user registered with role: USER",
                "INFO"
        );

        return saved;
    }

    public User registerUser(String username, String password, String role) {
        return registerUser(username, password);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Validates credentials. Throws a descriptive RuntimeException on failure
     * so the controller can map it to the correct HTTP status.
     *
     * Possible exceptions:
     *   "ACCOUNT_LOCKED"     – account locked (failed attempts or IP violation)
     *   "ACCOUNT_SUSPENDED"  – admin suspended the account
     *   "INVALID_CREDENTIALS"– wrong password
     */
    public User authenticate(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("INVALID_CREDENTIALS"));

        // Account status checks first — never leak which field failed
        if (user.isLocked()) {
            throw new RuntimeException("ACCOUNT_LOCKED");
        }
        if ("SUSPENDED".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("ACCOUNT_SUSPENDED");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            loginAttemptService.recordFailedAttempt(username);
            throw new RuntimeException("INVALID_CREDENTIALS");
        }

        // Successful login: reset failed attempt counter and update lastLoginAt
        loginAttemptService.resetAttempts(username);
        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
}
