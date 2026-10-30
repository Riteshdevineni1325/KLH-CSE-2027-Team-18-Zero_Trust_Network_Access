package com.ztna.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Access Policy for CAR-ZTNA.
 * Evaluated by the Policy Decision Point (PDP) using pure RBAC + ABAC logic.
 * Explicit ALLOW or DENY effect; NO numerical risk or trust scores.
 */
@Entity
@Table(name = "access_policies", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"policyName"})
})
public class AccessPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String policyName;

    @Column(nullable = false)
    private String resourceName;

    /**
     * RBAC constraint: USER, ADMIN, or ANY
     */
    @Column(nullable = false)
    private String requiredRole = "ANY";

    /**
     * ABAC constraint: OFFICE, HOME, or ANY
     */
    @Column(nullable = false)
    private String allowedNetwork = "ANY";

    /**
     * ABAC constraint: TRUSTED or ANY
     */
    @Column(nullable = false)
    private String requiredDeviceStatus = "ANY";

    /**
     * Decision effect: ALLOW or DENY
     */
    @Column(nullable = false)
    private String effect = "ALLOW";

    /**
     * Priority for conflict resolution: lower number = higher priority.
     * Note: Explicit DENY always takes precedence over ALLOW.
     */
    private int priority = 10;

    private boolean enabled = true;

    private String description;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    public AccessPolicy() {}

    public AccessPolicy(String policyName, String resourceName, String requiredRole,
                        String allowedNetwork, String requiredDeviceStatus,
                        String effect, int priority, String description) {
        this.policyName = policyName;
        this.resourceName = resourceName;
        this.requiredRole = requiredRole;
        this.allowedNetwork = allowedNetwork;
        this.requiredDeviceStatus = requiredDeviceStatus;
        this.effect = effect;
        this.priority = priority;
        this.description = description;
        this.enabled = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

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

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
