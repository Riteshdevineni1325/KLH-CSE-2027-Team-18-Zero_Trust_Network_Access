package com.ztna.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Immutable audit record of the context captured at every trust evaluation.
 * One row per evaluation event; never updated after insert.
 * Replaces the old ContextData entity.
 */
@Entity
@Table(name = "context_snapshots")
public class ContextSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    private String sessionId;

    private String ipAddress;

    private String deviceFingerprint;

    private String deviceInfo;

    /**
     * TRUSTED | UNREGISTERED | BLOCKED
     */
    private String deviceStatus = "UNREGISTERED";

    /**
     * OFFICE | HOME | UNKNOWN | BLOCKED
     */
    private String networkClassification = "UNKNOWN";

    private String requestedResource;

    /**
     * INTERNAL | EXTERNAL | VPN | BLOCKED
     */
    private String networkZone = "EXTERNAL";

    private LocalDateTime loginTime;

    private int hourOfDay;

    private int failedAttempts;

    /**
     * NORMAL | SUSPICIOUS
     */
    private String sessionActivity = "NORMAL";

    /**
     * Descriptive location hint (e.g. "Known office IP", "Unknown external IP")
     */
    private String locationHint;

    private LocalDateTime capturedAt;

    public ContextSnapshot() {}

    // ── Getters / Setters ───────────────────────────────────────

    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public String getDeviceStatus() { return deviceStatus; }
    public void setDeviceStatus(String deviceStatus) { this.deviceStatus = deviceStatus; }

    public String getNetworkZone() { return networkZone; }
    public void setNetworkZone(String networkZone) { this.networkZone = networkZone; }

    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }

    public int getHourOfDay() { return hourOfDay; }
    public void setHourOfDay(int hourOfDay) { this.hourOfDay = hourOfDay; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public String getSessionActivity() { return sessionActivity; }
    public void setSessionActivity(String sessionActivity) { this.sessionActivity = sessionActivity; }

    public String getLocationHint() { return locationHint; }
    public void setLocationHint(String locationHint) { this.locationHint = locationHint; }

    public String getNetworkClassification() { return networkClassification; }
    public void setNetworkClassification(String networkClassification) { this.networkClassification = networkClassification; }

    public String getRequestedResource() { return requestedResource; }
    public void setRequestedResource(String requestedResource) { this.requestedResource = requestedResource; }

    public LocalDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }
}
