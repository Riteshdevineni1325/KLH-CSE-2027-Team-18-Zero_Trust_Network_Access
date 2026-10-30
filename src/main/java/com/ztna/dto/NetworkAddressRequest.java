package com.ztna.dto;

public class NetworkAddressRequest {

    private String username;
    private String ipAddress;
    private String label;  // OFFICE | HOME | REMOTE | UNKNOWN
    private String zone;   // INTERNAL | EXTERNAL | VPN | BLOCKED
    private String status; // TRUSTED | UNKNOWN | BLOCKED

    public NetworkAddressRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
