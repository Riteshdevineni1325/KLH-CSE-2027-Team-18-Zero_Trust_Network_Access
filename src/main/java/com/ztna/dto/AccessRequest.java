package com.ztna.dto;

public class AccessRequest {

    private String username;
    private String resourceName;

    public AccessRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }
}
