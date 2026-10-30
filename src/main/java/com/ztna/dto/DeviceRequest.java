package com.ztna.dto;

public class DeviceRequest {

    private String username;
    private String deviceName;
    private String deviceType;
    private String operatingSystem;
    private String browser;
    private String ipAddress;
    // TRUSTED | UNREGISTERED | BLOCKED — defaults to UNREGISTERED if null
    private String status;

    public DeviceRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
