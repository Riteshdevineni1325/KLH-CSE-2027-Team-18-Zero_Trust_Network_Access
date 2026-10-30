package com.ztna.dto;

public class SuperKeyUnlockRequest {

    private String targetUsername;
    private String superKey;

    public SuperKeyUnlockRequest() {}

    public SuperKeyUnlockRequest(String targetUsername, String superKey) {
        this.targetUsername = targetUsername;
        this.superKey = superKey;
    }

    public String getTargetUsername() { return targetUsername; }
    public void setTargetUsername(String targetUsername) { this.targetUsername = targetUsername; }

    public String getSuperKey() { return superKey; }
    public void setSuperKey(String superKey) { this.superKey = superKey; }
}
