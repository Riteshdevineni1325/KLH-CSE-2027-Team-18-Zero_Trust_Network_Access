package com.ztna.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts")
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private int failedAttempts;

    private LocalDateTime lastAttemptTime;

    public LoginAttempt() {}

    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public LocalDateTime getLastAttemptTime() { return lastAttemptTime; }
    public void setLastAttemptTime(LocalDateTime lastAttemptTime) { this.lastAttemptTime = lastAttemptTime; }

    /**
     * Returns the time when the 24-hour penalty expires.
     * Returns null if there are no failed attempts or no timestamp set.
     */
    @Transient
    public LocalDateTime getPenaltyExpiresAt() {
        if (lastAttemptTime == null || failedAttempts == 0) return null;
        return lastAttemptTime.plusHours(24);
    }
}
