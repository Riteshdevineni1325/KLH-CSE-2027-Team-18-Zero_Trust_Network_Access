package com.ztna.dto;

import java.time.LocalDateTime;
import java.util.List;

public class RiskReportResponse {

    private String username;
    private int totalScore;
    private String riskLevel;
    private String decision;
    private LocalDateTime assessmentTime;

    // Context snapshot
    private String ipAddress;
    private String deviceInfo;
    private String deviceTrustStatus;
    private int failedLoginAttempts;
    private String sessionActivity;
    private LocalDateTime loginTime;

    // Per-factor breakdown
    private List<RiskFactor> factors;

    public RiskReportResponse() {}

    // ── Getters / Setters ──────────────────────────────────────

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public LocalDateTime getAssessmentTime() { return assessmentTime; }
    public void setAssessmentTime(LocalDateTime assessmentTime) { this.assessmentTime = assessmentTime; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public String getDeviceTrustStatus() { return deviceTrustStatus; }
    public void setDeviceTrustStatus(String deviceTrustStatus) { this.deviceTrustStatus = deviceTrustStatus; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public String getSessionActivity() { return sessionActivity; }
    public void setSessionActivity(String sessionActivity) { this.sessionActivity = sessionActivity; }

    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }

    public List<RiskFactor> getFactors() { return factors; }
    public void setFactors(List<RiskFactor> factors) { this.factors = factors; }

    // ── Inner class ────────────────────────────────────────────

    public static class RiskFactor {
        private String factor;
        private String observed;
        private int points;
        private String status; // OK, WARN, HIGH

        public RiskFactor(String factor, String observed, int points, String status) {
            this.factor = factor;
            this.observed = observed;
            this.points = points;
            this.status = status;
        }

        public String getFactor() { return factor; }
        public String getObserved() { return observed; }
        public int getPoints() { return points; }
        public String getStatus() { return status; }
    }
}
