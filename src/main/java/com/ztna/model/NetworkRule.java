package com.ztna.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Configurable Network Rule entity.
 * Supports static IP addresses and CIDR notation subnets.
 * Classifications: OFFICE, HOME, BLOCKED, UNKNOWN.
 */
@Entity
@Table(name = "network_rules")
public class NetworkRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ipOrCidr; // e.g. "192.168.1.0/24", "10.0.0.55", "127.0.0.1"

    /**
     * Classification: OFFICE | HOME | BLOCKED | UNKNOWN
     */
    @Column(nullable = false)
    private String classification;

    private String description;

    private boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    public NetworkRule() {}

    public NetworkRule(String ipOrCidr, String classification, String description) {
        this.ipOrCidr = ipOrCidr;
        this.classification = classification;
        this.description = description;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIpOrCidr() { return ipOrCidr; }
    public void setIpOrCidr(String ipOrCidr) { this.ipOrCidr = ipOrCidr; }

    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
