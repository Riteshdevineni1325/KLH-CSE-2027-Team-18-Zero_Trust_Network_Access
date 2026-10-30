package com.ztna.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Stable fingerprint derived at registration:
     * sha256(username + deviceName + os + browser) stored as hex.
     * Used for re-identification across sessions.
     */
    @Column(nullable = false)
    private String deviceFingerprint;

    @Column(nullable = false)
    private String deviceName;

    private String deviceType;      // Workstation | Mobile | Tablet | Server

    private String operatingSystem;

    private String browser;

    /**
     * Three-state device status.
     * TRUSTED        – registered and explicitly trusted by admin / user
     * UNREGISTERED   – seen but never registered
     * BLOCKED        – explicitly blocked; always scores max penalty
     */
    @Column(nullable = false)
    private String status = "UNREGISTERED"; // TRUSTED | UNREGISTERED | BLOCKED

    /**
     * Context label — where this device is typically used.
     * OFFICE  – device present at account registration
     * HOME    – any subsequent device seen for the first time
     * UNKNOWN – not yet classified
     */
    @Column(nullable = false)
    private String label = "UNKNOWN"; // OFFICE | HOME | UNKNOWN

    private LocalDateTime firstSeen;

    private LocalDateTime lastSeen;

    private LocalDateTime registeredAt;

    /** Optional admin notes */
    private String notes;

    public Device() {}

    // ── Getters / Setters ───────────────────────────────────────

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public LocalDateTime getFirstSeen() { return firstSeen; }
    public void setFirstSeen(LocalDateTime firstSeen) { this.firstSeen = firstSeen; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
