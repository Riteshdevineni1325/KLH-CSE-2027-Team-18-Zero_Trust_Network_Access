package com.ztna.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role = "USER"; // USER | ADMIN

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE | SUSPENDED | DELETED

    @Column(columnDefinition = "boolean default false")
    private boolean locked = false;

    // null = not locked by failed attempts or IP violation, else one of:
    // FAILED_ATTEMPTS | IP_VIOLATION | ADMIN_SUSPENDED
    private String lockReason;

    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Device> devices = new java.util.ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Session> sessions = new java.util.ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<NetworkAddress> networkAddresses = new java.util.ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<IpHistory> ipHistories = new java.util.ArrayList<>();

    public User() {
    }

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    // ── Getters / Setters ───────────────────────────────────────

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getLockReason() {
        return lockReason;
    }

    public void setLockReason(String lockReason) {
        this.lockReason = lockReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public java.util.List<NetworkAddress> getNetworkAddresses() {
        return networkAddresses;
    }

    public void setNetworkAddresses(java.util.List<NetworkAddress> networkAddresses) {
        this.networkAddresses = networkAddresses;
    }

    public java.util.List<Device> getDevices() {
        return devices;
    }

    public void setDevices(java.util.List<Device> devices) {
        this.devices = devices;
    }

    public java.util.List<Session> getSessions() {
        return sessions;
    }

    public void setSessions(java.util.List<Session> sessions) {
        this.sessions = sessions;
    }

    public java.util.List<IpHistory> getIpHistories() {
        return ipHistories;
    }

    public void setIpHistories(java.util.List<IpHistory> ipHistories) {
        this.ipHistories = ipHistories;
    }
}
