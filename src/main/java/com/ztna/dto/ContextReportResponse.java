package com.ztna.dto;

import com.ztna.model.SecurityLog;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Context & Security Posture Report.
 * Replaces numerical risk score breakdown with contextual zero-trust dimensions.
 */
public class ContextReportResponse {

    private String username;
    private String role;
    private String accountStatus; // ACTIVE | LOCKED
    private String ipAddress;
    private String networkClassification; // OFFICE | HOME | UNKNOWN | BLOCKED
    private String deviceInfo;
    private String deviceStatus; // TRUSTED | UNREGISTERED | BLOCKED
    private int failedLoginAttempts;
    private String lastDecision; // ALLOW | DENY
    private LocalDateTime lastSeen;
    private List<SecurityLog> recentAuditEvents;

    public ContextReportResponse() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getNetworkClassification() { return networkClassification; }
    public void setNetworkClassification(String networkClassification) { this.networkClassification = networkClassification; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public String getDeviceStatus() { return deviceStatus; }
    public void setDeviceStatus(String deviceStatus) { this.deviceStatus = deviceStatus; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public String getLastDecision() { return lastDecision; }
    public void setLastDecision(String lastDecision) { this.lastDecision = lastDecision; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    public List<SecurityLog> getRecentAuditEvents() { return recentAuditEvents; }
    public void setRecentAuditEvents(List<SecurityLog> recentAuditEvents) { this.recentAuditEvents = recentAuditEvents; }
}
