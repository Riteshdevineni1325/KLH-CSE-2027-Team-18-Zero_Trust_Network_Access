package com.ztna.service;

import com.ztna.model.LoginAttempt;
import com.ztna.model.User;
import com.ztna.repository.LoginAttemptRepository;
import com.ztna.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoginAttemptService {

    /** Five consecutive failures lock the account. */
    public static final int MAX_ATTEMPTS = 5;

    private final LoginAttemptRepository loginAttemptRepository;
    private final UserRepository userRepository;
    private final SecurityLogService securityLogService;

    public LoginAttemptService(LoginAttemptRepository loginAttemptRepository,
                               UserRepository userRepository,
                               SecurityLogService securityLogService) {
        this.loginAttemptRepository = loginAttemptRepository;
        this.userRepository = userRepository;
        this.securityLogService = securityLogService;
    }

    /**
     * Records a failed attempt. Locks the account once MAX_ATTEMPTS is reached.
     */
    public void recordFailedAttempt(String username) {
        LoginAttempt attempt = loginAttemptRepository.findByUsername(username)
                .orElseGet(() -> {
                    LoginAttempt a = new LoginAttempt();
                    a.setUsername(username);
                    a.setFailedAttempts(0);
                    return a;
                });

        int newCount = attempt.getFailedAttempts() + 1;
        attempt.setFailedAttempts(newCount);
        attempt.setLastAttemptTime(LocalDateTime.now());
        loginAttemptRepository.save(attempt);

        if (newCount >= MAX_ATTEMPTS) {
            lockAccount(username, "FAILED_ATTEMPTS");
        }
    }

    private void lockAccount(String username, String lockReason) {
        userRepository.findByUsername(username).ifPresent(user -> {
            if (!user.isLocked()) {
                user.setLocked(true);
                user.setStatus("LOCKED");
                user.setLockReason(lockReason);
                userRepository.save(user);

                securityLogService.logEvent(
                        username,
                        "ACCOUNT_LOCKED",
                        "SYSTEM",
                        "Account locked after " + MAX_ATTEMPTS +
                        " consecutive failed login attempts. Admin action required to unlock.",
                        "HIGH"
                );
            }
        });
    }

    public int getFailedAttempts(String username) {
        return loginAttemptRepository.findByUsername(username)
                .map(LoginAttempt::getFailedAttempts)
                .orElse(0);
    }

    public boolean isLocked(String username) {
        return userRepository.findByUsername(username)
                .map(User::isLocked)
                .orElse(false);
    }

    /**
     * Admin unlock: clears failed attempts, removes the lock flag and lockReason.
     */
    public void resetAttemptsByAdmin(String username) {
        loginAttemptRepository.findByUsername(username).ifPresent(attempt -> {
            attempt.setFailedAttempts(0);
            attempt.setLastAttemptTime(null);
            loginAttemptRepository.save(attempt);
        });

        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLocked(false);
            user.setStatus("ACTIVE");
            user.setLockReason(null);
            userRepository.save(user);
        });
    }

    /** Alias used internally (no admin audit trail). */
    public void resetAttempts(String username) {
        resetAttemptsByAdmin(username);
    }

    public void deleteAttemptsByUsername(String username) {
        loginAttemptRepository.findByUsername(username).ifPresent(loginAttemptRepository::delete);
    }
}
