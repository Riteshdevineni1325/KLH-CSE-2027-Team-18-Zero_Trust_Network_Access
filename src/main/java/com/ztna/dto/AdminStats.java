package com.ztna.dto;

/**
 * Aggregate dashboard statistics for CAR-ZTNA Administrator Dashboard.
 * Free of numerical risk/trust metrics; focuses on deterministic security states.
 */
public class AdminStats {

    private long totalUsers;
    private long activeUsers;
    private long lockedUsers;

    private long totalDevices;
    private long trustedDevices;
    private long unregisteredDevices;
    private long blockedDevices;

    private long activeSessions;

    private long totalNetworkRules;
    private long officeNetworks;
    private long homeNetworks;
    private long blockedNetworks;

    private long totalPolicies;
    private long allowPolicies;
    private long denyPolicies;

    private long totalSecurityLogs;
    private long criticalSecurityEvents;
    private long allowDecisions;
    private long denyDecisions;

    public AdminStats() {}

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }

    public long getLockedUsers() { return lockedUsers; }
    public void setLockedUsers(long lockedUsers) { this.lockedUsers = lockedUsers; }

    public long getTotalDevices() { return totalDevices; }
    public void setTotalDevices(long totalDevices) { this.totalDevices = totalDevices; }

    public long getTrustedDevices() { return trustedDevices; }
    public void setTrustedDevices(long trustedDevices) { this.trustedDevices = trustedDevices; }

    public long getUnregisteredDevices() { return unregisteredDevices; }
    public void setUnregisteredDevices(long unregisteredDevices) { this.unregisteredDevices = unregisteredDevices; }

    public long getBlockedDevices() { return blockedDevices; }
    public void setBlockedDevices(long blockedDevices) { this.blockedDevices = blockedDevices; }

    public long getActiveSessions() { return activeSessions; }
    public void setActiveSessions(long activeSessions) { this.activeSessions = activeSessions; }

    public long getTotalNetworkRules() { return totalNetworkRules; }
    public void setTotalNetworkRules(long totalNetworkRules) { this.totalNetworkRules = totalNetworkRules; }

    public long getOfficeNetworks() { return officeNetworks; }
    public void setOfficeNetworks(long officeNetworks) { this.officeNetworks = officeNetworks; }

    public long getHomeNetworks() { return homeNetworks; }
    public void setHomeNetworks(long homeNetworks) { this.homeNetworks = homeNetworks; }

    public long getBlockedNetworks() { return blockedNetworks; }
    public void setBlockedNetworks(long blockedNetworks) { this.blockedNetworks = blockedNetworks; }

    public long getTotalPolicies() { return totalPolicies; }
    public void setTotalPolicies(long totalPolicies) { this.totalPolicies = totalPolicies; }

    public long getAllowPolicies() { return allowPolicies; }
    public void setAllowPolicies(long allowPolicies) { this.allowPolicies = allowPolicies; }

    public long getDenyPolicies() { return denyPolicies; }
    public void setDenyPolicies(long denyPolicies) { this.denyPolicies = denyPolicies; }

    public long getTotalSecurityLogs() { return totalSecurityLogs; }
    public void setTotalSecurityLogs(long totalSecurityLogs) { this.totalSecurityLogs = totalSecurityLogs; }

    public long getCriticalSecurityEvents() { return criticalSecurityEvents; }
    public void setCriticalSecurityEvents(long criticalSecurityEvents) { this.criticalSecurityEvents = criticalSecurityEvents; }

    public long getAllowDecisions() { return allowDecisions; }
    public void setAllowDecisions(long allowDecisions) { this.allowDecisions = allowDecisions; }

    public long getDenyDecisions() { return denyDecisions; }
    public void setDenyDecisions(long denyDecisions) { this.denyDecisions = denyDecisions; }

    private String currentDeviceStatus = "UNKNOWN";
    private boolean deviceTrusted = false;

    public String getCurrentDeviceStatus() { return currentDeviceStatus; }
    public void setCurrentDeviceStatus(String currentDeviceStatus) {
        this.currentDeviceStatus = currentDeviceStatus;
        this.deviceTrusted = "TRUSTED".equalsIgnoreCase(currentDeviceStatus);
    }

    public boolean isDeviceTrusted() { return deviceTrusted; }
    public void setDeviceTrusted(boolean deviceTrusted) { this.deviceTrusted = deviceTrusted; }
}
