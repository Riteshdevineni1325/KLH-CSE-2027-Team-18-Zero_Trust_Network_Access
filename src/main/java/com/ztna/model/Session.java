package com.ztna.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sessionId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String ipAddress;

    private String deviceFingerprint;

    private String deviceInfo;

    private LocalDateTime loginTime;

    private LocalDateTime lastActivity;

    private LocalDateTime expiresAt;

    private boolean active = true;

    /**
     * Session state: ACTIVE | TERMINATED
     */
    @Column(nullable = false)
    private String status = "ACTIVE";

    private String networkClassification = "UNKNOWN";

    /**
     * Reason the session ended (null while still active).
     * LOGOUT | TIMEOUT | ACCOUNT_LOCKED | DEVICE_BLOCKED | NETWORK_BLOCKED | ADMIN
     */
    private String terminationReason;

    public Session() {}

    // ── Getters / Setters ───────────────────────────────────────

    public Long getId() { return id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }

    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) {
        this.active = active;
        this.status = active ? "ACTIVE" : "TERMINATED";
    }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        this.active = "ACTIVE".equalsIgnoreCase(status);
    }

    public String getNetworkClassification() { return networkClassification; }
    public void setNetworkClassification(String networkClassification) { this.networkClassification = networkClassification; }

    public String getTerminationReason() { return terminationReason; }
    public void setTerminationReason(String terminationReason) { this.terminationReason = terminationReason; }
}
