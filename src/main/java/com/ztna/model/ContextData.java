package com.ztna.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "context_data")
public class ContextData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String ipAddress;

    private String deviceInfo;

    private String deviceTrustStatus = "UNKNOWN";

    private String location;

    private LocalDateTime loginTime;

    private int failedLoginAttempts;

    private String sessionActivity;

    public ContextData() {
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getDeviceTrustStatus() {
        return deviceTrustStatus;
    }

    public void setDeviceTrustStatus(String deviceTrustStatus) {
        this.deviceTrustStatus = deviceTrustStatus;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public String getSessionActivity() {
        return sessionActivity;
    }

    public void setSessionActivity(String sessionActivity) {
        this.sessionActivity = sessionActivity;
    }
}