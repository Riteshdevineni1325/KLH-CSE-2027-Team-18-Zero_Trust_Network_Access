package com.ztna.dto;

public class AccessPolicyRequest {

    private Long id;
    private String policyName;
    private String resourceName;
    private String requiredRole = "ANY"; // USER | ADMIN | ANY
    private String allowedNetwork = "ANY"; // OFFICE | HOME | ANY
    private String requiredDeviceStatus = "ANY"; // TRUSTED | ANY
    private String effect = "ALLOW"; // ALLOW | DENY
    private int priority = 10;
    private String description;

    public AccessPolicyRequest() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public String getRequiredRole() { return requiredRole; }
    public void setRequiredRole(String requiredRole) { this.requiredRole = requiredRole; }

    public String getAllowedNetwork() { return allowedNetwork; }
    public void setAllowedNetwork(String allowedNetwork) { this.allowedNetwork = allowedNetwork; }

    public String getRequiredDeviceStatus() { return requiredDeviceStatus; }
    public void setRequiredDeviceStatus(String requiredDeviceStatus) { this.requiredDeviceStatus = requiredDeviceStatus; }

    public String getEffect() { return effect; }
    public void setEffect(String effect) { this.effect = effect; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
