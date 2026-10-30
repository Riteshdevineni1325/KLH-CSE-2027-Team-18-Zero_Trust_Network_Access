package com.ztna.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Replaces the hard-coded officeIp / homeIp columns on User.
 * Every IP address ever seen for a user is registered here with a
 * classification and a status so admins have full visibility and control.
 */
@Entity
@Table(name = "network_addresses",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "ip_address"}))
public class NetworkAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    /**
     * Human-readable label assigned either automatically or by admin.
     * OFFICE | HOME | REMOTE | UNKNOWN
     */
    private String label = "UNKNOWN";

    /**
     * Network zone classification.
     * INTERNAL | EXTERNAL | VPN | BLOCKED
     */
    private String zone = "EXTERNAL";

    /**
     * Access status.
     * TRUSTED | UNKNOWN | BLOCKED
     */
    private String status = "UNKNOWN";

    private LocalDateTime firstSeen;

    private LocalDateTime lastSeen;

    private LocalDateTime addedAt;

    private LocalDateTime blockedAt;

    private String blockedReason;

    public NetworkAddress() {}

    // ── Getters / Setters ───────────────────────────────────────

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getFirstSeen() { return firstSeen; }
    public void setFirstSeen(LocalDateTime firstSeen) { this.firstSeen = firstSeen; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }

    public LocalDateTime getBlockedAt() { return blockedAt; }
    public void setBlockedAt(LocalDateTime blockedAt) { this.blockedAt = blockedAt; }

    public String getBlockedReason() { return blockedReason; }
    public void setBlockedReason(String blockedReason) { this.blockedReason = blockedReason; }
}
