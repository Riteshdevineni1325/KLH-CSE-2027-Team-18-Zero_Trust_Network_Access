package com.ztna.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_promotion_requests")
public class AdminPromotionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String requesterUsername;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    private String reason;

    private String approvedByAdmin;

    private String secondAdminUsername;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;

    public AdminPromotionRequest() {}

    public AdminPromotionRequest(String requesterUsername, String reason) {
        this.requesterUsername = requesterUsername;
        this.reason = reason;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public String getRequesterUsername() { return requesterUsername; }
    public void setRequesterUsername(String requesterUsername) { this.requesterUsername = requesterUsername; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getApprovedByAdmin() { return approvedByAdmin; }
    public void setApprovedByAdmin(String approvedByAdmin) { this.approvedByAdmin = approvedByAdmin; }

    public String getSecondAdminUsername() { return secondAdminUsername; }
    public void setSecondAdminUsername(String secondAdminUsername) { this.secondAdminUsername = secondAdminUsername; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
