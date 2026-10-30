package com.ztna.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_logs")
public class SecurityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String eventType;

    private String ipAddress;

    private String sessionId;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * INFO | WARN | HIGH | CRITICAL
     */
    @Column(nullable = false)
    private String severity = "INFO";

    @Column(nullable = false)
    private LocalDateTime eventTime;

    public SecurityLog() {}

    public SecurityLog(String username, String eventType, String ipAddress,
                       String sessionId, String description, String severity) {
        this.username = username;
        this.eventType = eventType;
        this.ipAddress = ipAddress;
        this.sessionId = sessionId;
        this.description = description;
        this.severity = severity != null ? severity : "INFO";
        this.eventTime = LocalDateTime.now();
    }

    // ── Getters / Setters ───────────────────────────────────────

    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
}
