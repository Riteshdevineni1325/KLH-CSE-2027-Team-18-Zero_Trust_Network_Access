package com.ztna.dto;

import com.ztna.model.User;

/**
 * Context-aware Login Response.
 * Zero numerical scores or trust ratings; binary ALLOW or DENY decision with ambient context.
 */
public class LoginResponse {

    private User userData;
    private String decision; // ALLOW | DENY
    private String sessionId;
    private String networkClassification; // OFFICE | HOME | UNKNOWN | BLOCKED
    private String deviceStatus; // TRUSTED | UNREGISTERED | BLOCKED
    private String message;
    private String error;

    public LoginResponse() {}

    public LoginResponse(User userData, String decision, String sessionId,
                         String networkClassification, String deviceStatus, String message) {
        this.userData = userData;
        this.decision = decision;
        this.sessionId = sessionId;
        this.networkClassification = networkClassification;
        this.deviceStatus = deviceStatus;
        this.message = message;
    }

    public static LoginResponse denied(String error) {
        LoginResponse res = new LoginResponse();
        res.setDecision("DENY");
        res.setError(error);
        res.setMessage(error);
        return res;
    }

    public User getUserData() { return userData; }
    public void setUserData(User userData) { this.userData = userData; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getNetworkClassification() { return networkClassification; }
    public void setNetworkClassification(String networkClassification) { this.networkClassification = networkClassification; }

    public String getDeviceStatus() { return deviceStatus; }
    public void setDeviceStatus(String deviceStatus) { this.deviceStatus = deviceStatus; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
