package com.ztna.dto;

/**
 * Result of Policy Decision Point (PDP) evaluation.
 * Deterministic binary ALLOW or DENY decision with contextual audit reason.
 */
public class PolicyEvaluationResult {

    private String decision; // ALLOW | DENY
    private String policyName;
    private String resourceName;
    private String reason;
    private String networkClassification;
    private String deviceStatus;
    private String userRole;

    public PolicyEvaluationResult() {}

    public PolicyEvaluationResult(String decision, String policyName, String resourceName,
                                  String reason, String networkClassification,
                                  String deviceStatus, String userRole) {
        this.decision = decision;
        this.policyName = policyName;
        this.resourceName = resourceName;
        this.reason = reason;
        this.networkClassification = networkClassification;
        this.deviceStatus = deviceStatus;
        this.userRole = userRole;
    }

    public static PolicyEvaluationResult allow(String policyName, String resourceName, String reason,
                                               String networkClassification, String deviceStatus, String userRole) {
        return new PolicyEvaluationResult("ALLOW", policyName, resourceName, reason,
                networkClassification, deviceStatus, userRole);
    }

    public static PolicyEvaluationResult deny(String policyName, String resourceName, String reason,
                                              String networkClassification, String deviceStatus, String userRole) {
        return new PolicyEvaluationResult("DENY", policyName, resourceName, reason,
                networkClassification, deviceStatus, userRole);
    }

    public boolean isAllowed() {
        return "ALLOW".equalsIgnoreCase(decision);
    }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getNetworkClassification() { return networkClassification; }
    public void setNetworkClassification(String networkClassification) { this.networkClassification = networkClassification; }

    public String getDeviceStatus() { return deviceStatus; }
    public void setDeviceStatus(String deviceStatus) { this.deviceStatus = deviceStatus; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
}
