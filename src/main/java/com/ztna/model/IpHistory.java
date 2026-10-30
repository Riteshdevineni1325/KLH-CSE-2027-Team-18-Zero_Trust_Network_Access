package com.ztna.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Append-only log of every IP event observed for a user.
 * Never updated — one insert per observed event.
 */
@Entity
@Table(name = "ip_history")
public class IpHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String ipAddress;

    /**
     * LOGIN | ACCESS | FLAGGED | BLOCKED
     */
    @Column(nullable = false)
    private String eventType;

    private LocalDateTime seenAt;

    public IpHistory() {}

    public IpHistory(User user, String ipAddress, String eventType) {
        this.user = user;
        this.ipAddress = ipAddress;
        this.eventType = eventType;
        this.seenAt = LocalDateTime.now();
    }

    // ── Getters / Setters ───────────────────────────────────────

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public LocalDateTime getSeenAt() { return seenAt; }
    public void setSeenAt(LocalDateTime seenAt) { this.seenAt = seenAt; }
}
