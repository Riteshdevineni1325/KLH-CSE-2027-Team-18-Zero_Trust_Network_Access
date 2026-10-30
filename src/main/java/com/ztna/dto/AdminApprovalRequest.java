package com.ztna.dto;

public class AdminApprovalRequest {

    private String secondAdminUsername;
    private String secondAdminPassword;

    public AdminApprovalRequest() {}

    public AdminApprovalRequest(String secondAdminUsername, String secondAdminPassword) {
        this.secondAdminUsername = secondAdminUsername;
        this.secondAdminPassword = secondAdminPassword;
    }

    public String getSecondAdminUsername() { return secondAdminUsername; }
    public void setSecondAdminUsername(String secondAdminUsername) { this.secondAdminUsername = secondAdminUsername; }

    public String getSecondAdminPassword() { return secondAdminPassword; }
    public void setSecondAdminPassword(String secondAdminPassword) { this.secondAdminPassword = secondAdminPassword; }
}
